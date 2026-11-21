package main;

import core.ResultPresenter;
import core.*;

import java.util.StringJoiner;

public final class TextResultPresenter implements ResultPresenter {
    @Override
    public String present(OperationResult result) {
        StringJoiner output = new StringJoiner(System.lineSeparator());
        append(result, output);
        return output.toString();
    }

    private void append(OperationResult result, StringJoiner output) {
        if (result instanceof BriefResult brief) {
            output.add(
                    "Бриф "
                            + brief.orderId()
                            + ": "
                            + (brief.complete() ? "заполнен" : "неполный"));
            brief.errors().forEach(error -> output.add("  - " + error));
        } else if (result instanceof ReadinessResult readiness) {
            output.add(
                    "Этап "
                            + readiness.stageId()
                            + ": "
                            + (readiness.ready()
                                    ? "готов к согласованию"
                                    : "не готов к согласованию"));
            readiness
                    .versions()
                    .forEach(
                            version ->
                                    output.add(
                                            "  Материал "
                                                    + version.materialId()
                                                    + ": версия "
                                                    + version.versionId()));
            readiness.blockers().forEach(blocker -> output.add("  - " + blocker));
        } else if (result instanceof OverdueResult overdue) {
            output.add("Просроченные задачи на " + overdue.asOf() + ": " + overdue.tasks().size());
            overdue.tasks()
                    .forEach(
                            task ->
                                    output.add(
                                            "  "
                                                    + task.taskId()
                                                    + " | "
                                                    + task.title()
                                                    + " | срок "
                                                    + task.deadline()
                                                    + " | просрочка "
                                                    + task.daysOverdue()
                                                    + " дн."));
        } else if (result instanceof OrderSummary summary) {
            appendSummary(summary, output);
        } else if (result instanceof DemoResult demo) {
            append(demo.brief(), output);
            demo.readiness().forEach(item -> append(item, output));
            append(demo.overdue(), output);
            append(demo.summary(), output);
        } else {
            throw new IllegalArgumentException("неизвестный тип результата");
        }
    }

    private void appendSummary(OrderSummary summary, StringJoiner output) {
        var totals = summary.totals();
        output.add(
                "Сводка заказа "
                        + summary.order().id()
                        + " на "
                        + summary.asOf()
                        + ": "
                        + summary.order().title());
        output.add(
                "  Клиент: "
                        + summary.client().name()
                        + "; статус заказа: "
                        + summary.order().status());
        output.add(
                "  Этапов: "
                        + totals.stages()
                        + "; задач: "
                        + totals.tasks()
                        + "; завершено: "
                        + totals.doneTasks());
        output.add(
                "  Материалов: "
                        + totals.materials()
                        + "; версий: "
                        + totals.versions()
                        + "; решений клиента: "
                        + totals.approvals());
        output.add(
                "  Открытых замечаний: "
                        + totals.openComments()
                        + "; просроченных задач: "
                        + totals.overdueTasks());
        summary.team()
                .forEach(member -> output.add("  " + member.name() + ": " + member.projectRole()));
        summary.approvals()
                .forEach(
                        approval ->
                                output.add(
                                        "  Решение "
                                                + approval.id()
                                                + " по версии "
                                                + approval.versionId()
                                                + ": "
                                                + approval.decision()
                                                + " ("
                                                + approval.decidedOn()
                                                + ")"));
    }
}
