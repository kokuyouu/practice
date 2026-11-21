package core;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class OverdueTaskFinder {
    public OverdueResult find(CampaignData data, LocalDate asOf, String orderId) {
        if (orderId != null) {
            data.order(orderId);
        }
        List<String> activeOrders = new ArrayList<>();
        for (Order order : data.orders()) {
            if (!order.isClosed() && (orderId == null || order.id().equals(orderId))) {
                activeOrders.add(order.id());
            }
        }
        activeOrders.sort(TextValues::compare);
        List<Stage> stages = new ArrayList<>();
        for (Stage stage : data.stages()) {
            if (Collections.binarySearch(activeOrders, stage.orderId(), TextValues::compare) >= 0) {
                stages.add(stage);
            }
        }
        stages.sort(Comparator.comparing(Stage::id, TextValues::compare));
        List<String> stageIds = stages.stream().map(Stage::id).toList();
        List<OverdueResult.Item> overdue = new ArrayList<>();
        for (Task task : data.tasks()) {
            int position = Collections.binarySearch(stageIds, task.stageId(), TextValues::compare);
            if (position < 0 || !task.isUnfinished() || !task.deadline().isBefore(asOf)) {
                continue;
            }
            overdue.add(
                    new OverdueResult.Item(
                            task.id(),
                            task.title(),
                            stages.get(position).orderId(),
                            task.stageId(),
                            task.employeeId(),
                            task.deadline().toString(),
                            ChronoUnit.DAYS.between(task.deadline(), asOf)));
        }
        overdue.sort(
                Comparator.comparing(OverdueResult.Item::deadline)
                        .thenComparing(OverdueResult.Item::taskId, TextValues::compare));
        return new OverdueResult(asOf.toString(), orderId, overdue);
    }
}
