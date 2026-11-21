package core;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class CampaignService {
    private final CampaignData data;
    private final BriefValidator briefValidator;
    private final StageReadinessChecker readinessChecker;
    private final OverdueTaskFinder overdueFinder;

    public CampaignService(
            CampaignData data,
            BriefValidator briefValidator,
            StageReadinessChecker readinessChecker,
            OverdueTaskFinder overdueFinder) {
        this.data = data;
        this.briefValidator = briefValidator;
        this.readinessChecker = readinessChecker;
        this.overdueFinder = overdueFinder;
    }

    public BriefResult checkBrief(String orderId) {
        return briefValidator.check(data.order(orderId));
    }

    public ReadinessResult checkStageReadiness(String stageId) {
        return readinessChecker.check(data, stageId);
    }

    public OverdueResult findOverdueTasks(LocalDate asOf, String orderId) {
        return overdueFinder.find(data, asOf, orderId);
    }

    public OrderSummary buildOrderSummary(String orderId, LocalDate asOf) {
        Order order = data.order(orderId);
        var stages =
                data.stages().stream().filter(stage -> stage.orderId().equals(orderId)).toList();
        Set<String> stageIds = stages.stream().map(Stage::id).collect(Collectors.toSet());
        var tasks =
                data.tasks().stream().filter(task -> stageIds.contains(task.stageId())).toList();
        var materials =
                data.materials().stream()
                        .filter(material -> stageIds.contains(material.stageId()))
                        .toList();
        Set<String> materialIds = materials.stream().map(Material::id).collect(Collectors.toSet());
        var versions =
                data.materialVersions().stream()
                        .filter(version -> materialIds.contains(version.materialId()))
                        .toList();
        Set<String> versionIds =
                versions.stream().map(MaterialVersion::id).collect(Collectors.toSet());
        var approvals =
                data.approvals().stream()
                        .filter(approval -> versionIds.contains(approval.versionId()))
                        .toList();
        var comments =
                data.comments().stream()
                        .filter(comment -> versionIds.contains(comment.versionId()))
                        .toList();
        Map<String, Employee> employees = new HashMap<>();
        for (Employee employee : data.employees()) {
            employees.put(employee.id(), employee);
        }
        var team = new ArrayList<OrderSummary.TeamMember>();
        for (OrderMember membership : data.orderMembers()) {
            if (membership.orderId().equals(orderId)) {
                Employee employee = employees.get(membership.employeeId());
                team.add(
                        new OrderSummary.TeamMember(
                                employee.id(), employee.name(), membership.projectRole()));
            }
        }
        var overdue = findOverdueTasks(asOf, orderId).tasks();
        var totals =
                new OrderSummary.Totals(
                        stages.size(),
                        tasks.size(),
                        tasks.stream().filter(task -> task.status().equals("done")).count(),
                        materials.size(),
                        versions.size(),
                        approvals.size(),
                        comments.stream().filter(Comment::isOpen).count(),
                        overdue.size());
        return new OrderSummary(
                asOf.toString(),
                order,
                data.client(order.clientId()),
                team,
                stages,
                tasks,
                materials,
                versions,
                approvals,
                comments,
                overdue,
                totals);
    }

    public DemoResult demo(String orderId, LocalDate asOf) {
        data.order(orderId);
        var readiness =
                data.stages().stream()
                        .filter(stage -> stage.orderId().equals(orderId))
                        .map(stage -> checkStageReadiness(stage.id()))
                        .toList();
        return new DemoResult(
                checkBrief(orderId),
                readiness,
                findOverdueTasks(asOf, orderId),
                buildOrderSummary(orderId, asOf));
    }
}
