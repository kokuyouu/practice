package main;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import core.ResultPresenter;
import core.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class JsonResultPresenter implements ResultPresenter {
    private final Gson gson;
    private final Map<String, Map<String, JsonObject>> originalRows;

    public JsonResultPresenter(JsonObject original) {
        gson =
                new GsonBuilder()
                        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                        .serializeNulls()
                        .disableHtmlEscaping()
                        .setPrettyPrinting()
                        .create();
        originalRows = new HashMap<>();
        for (String table :
                List.of(
                        "clients",
                        "orders",
                        "stages",
                        "tasks",
                        "materials",
                        "material_versions",
                        "approvals",
                        "comments")) {
            Map<String, JsonObject> rows = new HashMap<>();
            for (JsonElement element : original.getAsJsonArray(table)) {
                JsonObject row = element.getAsJsonObject();
                rows.put(row.get("id").getAsString(), row.deepCopy());
            }
            originalRows.put(table, rows);
        }
    }

    @Override
    public String present(OperationResult result) {
        return gson.toJson(toJson(result));
    }

    private JsonElement toJson(OperationResult result) {
        if (result instanceof OrderSummary summary) {
            return summaryJson(summary);
        }
        if (result instanceof DemoResult demo) {
            JsonObject json = new JsonObject();
            json.add("brief", toJson(demo.brief()));
            json.add("readiness", gson.toJsonTree(demo.readiness()));
            json.add("overdue", toJson(demo.overdue()));
            json.add("summary", summaryJson(demo.summary()));
            return json;
        }
        return gson.toJsonTree(result);
    }

    private JsonObject summaryJson(OrderSummary summary) {
        JsonObject json = new JsonObject();
        json.addProperty("as_of", summary.asOf());
        json.add("order", originalRows.get("orders").get(summary.order().id()));
        json.add("client", originalRows.get("clients").get(summary.client().id()));
        json.add("team", gson.toJsonTree(summary.team()));
        json.add("stages", rows("stages", summary.stages(), item -> item.id()));
        json.add("tasks", rows("tasks", summary.tasks(), item -> item.id()));
        json.add("materials", rows("materials", summary.materials(), item -> item.id()));
        json.add("versions", rows("material_versions", summary.versions(), item -> item.id()));
        json.add("approvals", rows("approvals", summary.approvals(), item -> item.id()));
        json.add("comments", rows("comments", summary.comments(), item -> item.id()));
        json.add("overdue_tasks", gson.toJsonTree(summary.overdueTasks()));
        json.add("totals", gson.toJsonTree(summary.totals()));
        return json;
    }

    private <T> JsonArray rows(String table, List<T> records, Function<T, String> identifier) {
        JsonArray result = new JsonArray();
        for (T record : records) {
            result.add(originalRows.get(table).get(identifier.apply(record)));
        }
        return result;
    }
}
