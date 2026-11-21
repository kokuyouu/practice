package core;


import java.util.List;

public record OrderSummary(
        String asOf,
        Order order,
        Client client,
        List<TeamMember> team,
        List<Stage> stages,
        List<Task> tasks,
        List<Material> materials,
        List<MaterialVersion> versions,
        List<Approval> approvals,
        List<Comment> comments,
        List<OverdueResult.Item> overdueTasks,
        Totals totals)
        implements OperationResult {
    public OrderSummary {
        team = List.copyOf(team);
        stages = List.copyOf(stages);
        tasks = List.copyOf(tasks);
        materials = List.copyOf(materials);
        versions = List.copyOf(versions);
        approvals = List.copyOf(approvals);
        comments = List.copyOf(comments);
        overdueTasks = List.copyOf(overdueTasks);
    }

    public record TeamMember(String employeeId, String name, String projectRole) {}

    public record Totals(
            int stages,
            int tasks,
            long doneTasks,
            int materials,
            int versions,
            int approvals,
            long openComments,
            int overdueTasks) {}
}
