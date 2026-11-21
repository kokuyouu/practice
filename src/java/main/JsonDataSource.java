package main;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import core.CampaignDataSource;
import core.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class JsonDataSource implements CampaignDataSource {
    private final Path file;
    private JsonObject original;

    public JsonDataSource(Path file) {
        this.file = file;
    }

    @Override
    public CampaignData load() throws IOException {
        JsonElement parsed;
        try (Reader reader = open();
                JsonReader jsonReader = new JsonReader(reader)) {
            jsonReader.setStrictness(Strictness.STRICT);
            parsed = JsonParser.parseReader(jsonReader);
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("после объекта JSON обнаружены лишние данные");
            }
        }
        if (!parsed.isJsonObject()) {
            throw new IllegalArgumentException("корень JSON должен быть объектом");
        }
        JsonObject root = parsed.getAsJsonObject();
        validate(root);
        CampaignData data =
                new CampaignData(
                        rows(
                                root,
                                "clients",
                                row ->
                                        new Client(
                                                text(row, "id"),
                                                text(row, "name"),
                                                text(row, "contact"))),
                        rows(root, "orders", this::order),
                        rows(
                                root,
                                "employees",
                                row ->
                                        new Employee(
                                                text(row, "id"),
                                                text(row, "name"),
                                                text(row, "specialty"))),
                        rows(
                                root,
                                "order_members",
                                row ->
                                        new OrderMember(
                                                text(row, "order_id"),
                                                text(row, "employee_id"),
                                                text(row, "project_role"))),
                        rows(
                                root,
                                "stages",
                                row ->
                                        new Stage(
                                                text(row, "id"),
                                                text(row, "order_id"),
                                                text(row, "name"),
                                                text(row, "status"))),
                        rows(root, "tasks", this::task),
                        rows(
                                root,
                                "materials",
                                row ->
                                        new Material(
                                                text(row, "id"),
                                                text(row, "stage_id"),
                                                text(row, "name"))),
                        rows(
                                root,
                                "material_versions",
                                row ->
                                        new MaterialVersion(
                                                text(row, "id"),
                                                text(row, "material_id"),
                                                new BigInteger(row.get("number").getAsString()),
                                                text(row, "url"),
                                                text(row, "review_status"))),
                        rows(
                                root,
                                "task_materials",
                                row ->
                                        new TaskMaterial(
                                                text(row, "task_id"), text(row, "material_id"))),
                        rows(
                                root,
                                "approvals",
                                row ->
                                        new Approval(
                                                text(row, "id"),
                                                text(row, "version_id"),
                                                text(row, "decision"),
                                                DateValues.parse(text(row, "decided_on")))),
                        rows(
                                root,
                                "comments",
                                row ->
                                        new Comment(
                                                text(row, "id"),
                                                text(row, "version_id"),
                                                text(row, "text"),
                                                text(row, "status"))));
        data.validateContext();
        original = root.deepCopy();
        return data;
    }

    public JsonObject originalData() {
        if (original == null) {
            throw new IllegalStateException("данные ещё не загружены");
        }
        return original.deepCopy();
    }

    private Reader open() throws IOException {
        if (file != null) {
            return Files.newBufferedReader(file, StandardCharsets.UTF_8);
        }
        InputStream input = JsonDataSource.class.getResourceAsStream("/sample_data.json");
        if (input == null) {
            throw new IOException("sample_data.json отсутствует в ресурсах программы");
        }
        return new InputStreamReader(input, StandardCharsets.UTF_8);
    }

    private Order order(JsonObject row) {
        return new Order(
                text(row, "id"),
                text(row, "client_id"),
                text(row, "title"),
                text(row, "goal"),
                text(row, "audience"),
                text(row, "format"),
                text(row, "contact"),
                amount(row.get("budget")),
                text(row, "deadline"),
                text(row, "status"));
    }

    private Task task(JsonObject row) {
        String commentId = text(row, "comment_id");
        if (commentId != null) {
            return new ReworkTask(
                    text(row, "id"),
                    text(row, "stage_id"),
                    text(row, "employee_id"),
                    text(row, "title"),
                    DateValues.parse(text(row, "deadline")),
                    text(row, "status"),
                    row.get("required").getAsBoolean(),
                    commentId);
        }
        return new Task(
                text(row, "id"),
                text(row, "stage_id"),
                text(row, "employee_id"),
                text(row, "title"),
                DateValues.parse(text(row, "deadline")),
                text(row, "status"),
                row.get("required").getAsBoolean());
    }

    private Number amount(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        String literal = value.getAsString();
        return literal.matches("-?[0-9]+") ? new BigInteger(literal) : Double.valueOf(literal);
    }

    private static String text(JsonObject row, String field) {
        JsonElement value = row.get(field);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
                ? value.getAsString()
                : null;
    }

    private static <T> List<T> rows(JsonObject root, String table, Function<JsonObject, T> mapper) {
        List<T> result = new ArrayList<>();
        for (JsonElement row : root.getAsJsonArray(table)) {
            result.add(mapper.apply(row.getAsJsonObject()));
        }
        return result;
    }

    private void validate(JsonObject root) {
        List<String> tables =
                List.of(
                        "clients",
                        "orders",
                        "employees",
                        "order_members",
                        "stages",
                        "tasks",
                        "materials",
                        "material_versions",
                        "task_materials",
                        "approvals",
                        "comments");
        Map<String, Set<String>> identifiers = new HashMap<>();
        for (String table : tables) {
            JsonElement value = root.get(table);
            if (value == null || !value.isJsonArray()) {
                throw new IllegalArgumentException(table + ": ожидается список записей");
            }
            Set<String> ids = new HashSet<>();
            for (JsonElement element : value.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    throw new IllegalArgumentException(table + ": ожидается список записей");
                }
                if (table.equals("order_members") || table.equals("task_materials")) {
                    continue;
                }
                String id = text(element.getAsJsonObject(), "id");
                if (id == null || TextValues.isBlank(id)) {
                    throw new IllegalArgumentException(table + ": нужен непустой строковый id");
                }
                if (!ids.add(id)) {
                    throw new IllegalArgumentException(table + ": повторяющийся id '" + id + "'");
                }
            }
            identifiers.put(table, ids);
        }
        Map<String, List<String>> required =
                Map.ofEntries(
                        Map.entry("clients", List.of("name", "contact")),
                        Map.entry("orders", List.of("title")),
                        Map.entry("employees", List.of("name", "specialty")),
                        Map.entry("order_members", List.of("project_role")),
                        Map.entry("stages", List.of("name")),
                        Map.entry("tasks", List.of("title")),
                        Map.entry("materials", List.of("name")),
                        Map.entry("material_versions", List.of("url")),
                        Map.entry("comments", List.of("text")));
        for (String table : tables) {
            for (JsonElement element : root.getAsJsonArray(table)) {
                JsonObject row = element.getAsJsonObject();
                for (String field : required.getOrDefault(table, List.of())) {
                    String value = text(row, field);
                    if (value == null || TextValues.isBlank(value)) {
                        throw new IllegalArgumentException(
                                table + ": поле " + field + " должно быть заполнено");
                    }
                }
            }
        }
        List<Reference> references =
                List.of(
                        new Reference("orders", "client_id", "clients"),
                        new Reference("order_members", "order_id", "orders"),
                        new Reference("order_members", "employee_id", "employees"),
                        new Reference("stages", "order_id", "orders"),
                        new Reference("tasks", "stage_id", "stages"),
                        new Reference("tasks", "employee_id", "employees"),
                        new Reference("materials", "stage_id", "stages"),
                        new Reference("material_versions", "material_id", "materials"),
                        new Reference("task_materials", "task_id", "tasks"),
                        new Reference("task_materials", "material_id", "materials"),
                        new Reference("approvals", "version_id", "material_versions"),
                        new Reference("comments", "version_id", "material_versions"));
        for (Reference reference : references) {
            for (JsonElement element : root.getAsJsonArray(reference.table())) {
                String value = text(element.getAsJsonObject(), reference.field());
                if (value == null || !identifiers.get(reference.target()).contains(value)) {
                    throw new IllegalArgumentException(
                            reference.table() + ": неверная ссылка " + reference.field());
                }
            }
        }
        Map<String, List<String>> states =
                Map.of(
                        "orders", List.of("new", "in_progress", "completed", "cancelled"),
                        "stages",
                                List.of(
                                        "planned",
                                        "in_progress",
                                        "review",
                                        "approved",
                                        "completed"),
                        "tasks", List.of("todo", "in_progress", "done", "cancelled"),
                        "material_versions", List.of("pending", "passed", "failed"),
                        "approvals", List.of("approved", "changes_requested", "rejected"),
                        "comments", List.of("open", "resolved"));
        for (String table : tables) {
            if (!states.containsKey(table)) {
                continue;
            }
            String field =
                    table.equals("material_versions")
                            ? "review_status"
                            : table.equals("approvals") ? "decision" : "status";
            for (JsonElement element : root.getAsJsonArray(table)) {
                String value = text(element.getAsJsonObject(), field);
                if (value == null || !states.get(table).contains(value)) {
                    throw new IllegalArgumentException(table + ": неизвестное значение " + field);
                }
            }
        }
        requireUniquePairs(
                root.getAsJsonArray("order_members"), "order_id", "employee_id", "order_members");
        requireUniquePairs(
                root.getAsJsonArray("task_materials"), "task_id", "material_id", "task_materials");
        Set<List<String>> versionNumbers = new HashSet<>();
        for (JsonElement element : root.getAsJsonArray("material_versions")) {
            JsonObject row = element.getAsJsonObject();
            JsonElement number = row.get("number");
            if (number == null
                    || !number.isJsonPrimitive()
                    || !number.getAsJsonPrimitive().isNumber()
                    || !number.getAsString().matches("[0-9]+")
                    || new BigInteger(number.getAsString()).signum() < 1) {
                throw new IllegalArgumentException(
                        "material_versions: number должен быть положительным целым");
            }
            List<String> key = List.of(text(row, "material_id"), number.getAsString());
            if (!versionNumbers.add(key)) {
                throw new IllegalArgumentException(
                        "material_versions: повторный номер версии материала");
            }
        }
        for (JsonElement element : root.getAsJsonArray("tasks")) {
            JsonObject row = element.getAsJsonObject();
            JsonElement requiredValue = row.get("required");
            if (requiredValue == null
                    || !requiredValue.isJsonPrimitive()
                    || !requiredValue.getAsJsonPrimitive().isBoolean()) {
                throw new IllegalArgumentException(
                        "tasks: required должен быть логическим значением");
            }
            DateValues.parse(text(row, "deadline"));
            JsonElement comment = row.get("comment_id");
            if (comment != null
                    && !comment.isJsonNull()
                    && (text(row, "comment_id") == null
                            || !identifiers.get("comments").contains(text(row, "comment_id")))) {
                throw new IllegalArgumentException("tasks: неверная ссылка comment_id");
            }
        }
        for (JsonElement element : root.getAsJsonArray("approvals")) {
            DateValues.parse(text(element.getAsJsonObject(), "decided_on"));
        }
    }

    private void requireUniquePairs(JsonArray rows, String first, String second, String table) {
        Set<List<String>> pairs = new HashSet<>();
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            if (!pairs.add(List.of(text(row, first), text(row, second)))) {
                throw new IllegalArgumentException(table + ": повторная связь");
            }
        }
    }

    private record Reference(String table, String field, String target) {}
}
