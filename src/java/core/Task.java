package core;

import java.time.LocalDate;

public class Task {
    private final String id;
    private final String stageId;
    private final String employeeId;
    private final String title;
    private final LocalDate deadline;
    private final String status;
    private final boolean required;

    public Task(
            String id,
            String stageId,
            String employeeId,
            String title,
            LocalDate deadline,
            String status,
            boolean required) {
        this.id = id;
        this.stageId = stageId;
        this.employeeId = employeeId;
        this.title = title;
        this.deadline = deadline;
        this.status = status;
        this.required = required;
    }

    public String id() {
        return id;
    }

    public String stageId() {
        return stageId;
    }

    public String employeeId() {
        return employeeId;
    }

    public String title() {
        return title;
    }

    public LocalDate deadline() {
        return deadline;
    }

    public String status() {
        return status;
    }

    public boolean required() {
        return required;
    }

    public boolean isUnfinished() {
        return !status.equals("done") && !status.equals("cancelled");
    }

    public void validateContext(CampaignData data) {
        String orderId = data.stage(stageId).orderId();
        boolean assigned =
                data.orderMembers().stream()
                        .anyMatch(
                                member ->
                                        member.orderId().equals(orderId)
                                                && member.employeeId().equals(employeeId));
        if (!assigned) {
            throw new IllegalArgumentException(
                    "tasks: исполнитель " + id + " не входит в команду заказа");
        }
    }
}
