import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import core.Order;
import core.BriefRule;
import core.BriefValidator;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class ContractTest {
    @TempDir private Path directory;

    @Test
    void sampleDemoMatchesPythonGoldenResult() throws Exception {
        JsonElement expected;
        try (var reader =
                new InputStreamReader(
                        getClass().getResourceAsStream("/expected_demo.json"),
                        StandardCharsets.UTF_8)) {
            expected = JsonParser.parseReader(reader);
        }
        Response response = invoke(null, "demo", "--order", "O1", "--as-of", "2026-10-31");
        assertEquals(0, response.exitCode());
        assertEquals(expected, JsonParser.parseString(response.output()));
    }

    @Test
    void latestVersionIsSelectedByNumberAndBlocksOldApproval() throws Exception {
        JsonObject input = sample();
        JsonObject latest = input.getAsJsonArray("material_versions").get(1).getAsJsonObject();
        latest.addProperty("id", "V0");
        latest.addProperty("review_status", "failed");
        JsonArray reversed = new JsonArray();
        reversed.add(latest);
        reversed.add(input.getAsJsonArray("material_versions").get(0));
        reversed.add(input.getAsJsonArray("material_versions").get(2));
        input.add("material_versions", reversed);
        JsonObject result = json(invoke(input, "readiness", "--stage", "S1"));
        assertFalse(result.get("ready").getAsBoolean());
        assertEquals(
                "V0",
                result.getAsJsonArray("versions")
                        .get(0)
                        .getAsJsonObject()
                        .get("version_id")
                        .getAsString());
        assertEquals(1, result.getAsJsonArray("blockers").size());
    }

    @Test
    void dueTodayAndClosedOrdersAreExcluded() throws Exception {
        JsonObject input = sample();
        input.getAsJsonArray("orders").get(1).getAsJsonObject().addProperty("status", "cancelled");
        input.getAsJsonArray("tasks")
                .get(2)
                .getAsJsonObject()
                .addProperty("deadline", "2026-10-31");
        assertEquals(
                0,
                json(invoke(input, "overdue", "--as-of", "2026-10-31"))
                        .getAsJsonArray("tasks")
                        .size());
    }

    @Test
    void reworkTaskAddsValidationOfItsCommentStage() throws Exception {
        JsonObject input = sample();
        input.getAsJsonArray("tasks").get(2).getAsJsonObject().addProperty("comment_id", "CM1");
        Response response = invoke(input, "demo");
        assertEquals(2, response.exitCode());
        assertTrue(response.error().contains("замечание относится к другому этапу"));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "employee",
                "required",
                "duplicate-id",
                "version-number",
                "cross-stage",
                "missing-table"
            })
    void malformedEntitySetsAreRejected(String fault) throws Exception {
        JsonObject input = sample();
        switch (fault) {
            case "employee" ->
                    input.getAsJsonArray("tasks")
                            .get(0)
                            .getAsJsonObject()
                            .addProperty("employee_id", "absent");
            case "required" ->
                    input.getAsJsonArray("tasks")
                            .get(0)
                            .getAsJsonObject()
                            .addProperty("required", "true");
            case "duplicate-id" ->
                    input.getAsJsonArray("tasks")
                            .add(input.getAsJsonArray("tasks").get(0).deepCopy());
            case "version-number" ->
                    input.getAsJsonArray("material_versions")
                            .get(1)
                            .getAsJsonObject()
                            .addProperty("number", 1);
            case "cross-stage" ->
                    input.getAsJsonArray("task_materials")
                            .get(0)
                            .getAsJsonObject()
                            .addProperty("material_id", "M2");
            case "missing-table" -> input.remove("comments");
            default -> fail("unknown fixture");
        }
        Response response = invoke(input, "demo");
        assertEquals(2, response.exitCode());
        assertTrue(response.error().startsWith("Ошибка: "));
        assertTrue(response.output().isEmpty());
    }

    @Test
    void incompleteBriefReturnsAllDiagnostics() throws Exception {
        JsonObject input = sample();
        JsonObject order = input.getAsJsonArray("orders").get(0).getAsJsonObject();
        order.addProperty("goal", true);
        order.addProperty("audience", "\u00a0");
        order.addProperty("budget", false);
        order.addProperty("deadline", "2026-02-30");
        JsonObject result = json(invoke(input, "brief", "--order", "O1"));
        assertFalse(result.get("complete").getAsBoolean());
        assertEquals(4, result.getAsJsonArray("errors").size());
    }

    @Test
    void summaryPreservesExtraAndNonStringBriefAttributes() throws Exception {
        JsonObject input = sample();
        JsonObject order = input.getAsJsonArray("orders").get(0).getAsJsonObject();
        order.addProperty("goal", 42);
        order.add("extra", JsonParser.parseString("{\"tags\":[\"custom\"],\"active\":true}"));
        assertEquals(
                order,
                json(invoke(input, "summary", "--order", "O1", "--as-of", "2026-10-31"))
                        .get("order"));
    }

    @Test
    void supplementaryUnicodeIdentifiersUsePythonCodePointOrder() throws Exception {
        JsonObject input = sample();
        String first = "\ue000";
        String second = new String(Character.toChars(0x10000));
        input.getAsJsonArray("tasks").get(2).getAsJsonObject().addProperty("id", first);
        input.getAsJsonArray("tasks").get(4).getAsJsonObject().addProperty("id", second);
        input.getAsJsonArray("tasks")
                .get(4)
                .getAsJsonObject()
                .addProperty("deadline", "2026-10-30");
        input.getAsJsonArray("task_materials")
                .get(2)
                .getAsJsonObject()
                .addProperty("task_id", first);
        input.getAsJsonArray("task_materials")
                .get(3)
                .getAsJsonObject()
                .addProperty("task_id", second);
        JsonArray tasks =
                json(invoke(input, "overdue", "--as-of", "2026-10-31")).getAsJsonArray("tasks");
        assertEquals(first, tasks.get(0).getAsJsonObject().get("task_id").getAsString());
        assertEquals(second, tasks.get(1).getAsJsonObject().get("task_id").getAsString());
    }

    @Test
    void aNewBriefRuleIsInjectedWithoutChangingTheValidator() {
        BriefRule maximumBudget =
                order ->
                        order.budget().doubleValue() > 500_000
                                ? List.of("Бюджет требует отдельного согласования.")
                                : List.of();
        var validator = new BriefValidator(List.of(maximumBudget));
        var order =
                new Order(
                        "O9",
                        "C1",
                        "Пример",
                        "Цель",
                        "Аудитория",
                        "Формат",
                        "Контакт",
                        BigInteger.valueOf(600_000),
                        "2026-11-30",
                        "new");
        assertEquals(
                List.of("Бюджет требует отдельного согласования."),
                validator.check(order).errors());
    }

    @Test
    void unknownStageAndInvalidDateReturnExitCodeTwo() throws Exception {
        assertEquals(2, invoke(null, "readiness", "--stage", "absent").exitCode());
        assertEquals(2, invoke(null, "overdue", "--as-of", "2026-02-30").exitCode());
    }

    private JsonObject sample() throws Exception {
        try (var reader =
                new InputStreamReader(
                        getClass().getResourceAsStream("/sample_data.json"),
                        StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private JsonObject json(Response response) {
        assertEquals(0, response.exitCode(), response.error());
        return JsonParser.parseString(response.output()).getAsJsonObject();
    }

    private Response invoke(JsonObject input, String... command) throws Exception {
        var arguments = new ArrayList<String>();
        arguments.add("--json");
        if (input != null) {
            Path file = directory.resolve("data.json");
            Files.writeString(file, input.toString());
            arguments.addAll(List.of("--data", file.toString()));
        }
        arguments.addAll(List.of(command));
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        int exitCode =
                new Main()
                        .run(
                                arguments.toArray(String[]::new),
                                new PrintStream(output, true, StandardCharsets.UTF_8),
                                new PrintStream(errors, true, StandardCharsets.UTF_8));
        return new Response(
                exitCode,
                output.toString(StandardCharsets.UTF_8),
                errors.toString(StandardCharsets.UTF_8));
    }

    private record Response(int exitCode, String output, String error) {}
}
