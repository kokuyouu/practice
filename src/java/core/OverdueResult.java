package core;

import java.util.List;

public record OverdueResult(String asOf, String orderId, List<Item> tasks)
        implements OperationResult {
    public OverdueResult {
        tasks = List.copyOf(tasks);
    }

    public record Item(
            String taskId,
            String title,
            String orderId,
            String stageId,
            String employeeId,
            String deadline,
            long daysOverdue) {}
}
