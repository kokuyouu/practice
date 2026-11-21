package core;


import java.util.ArrayList;
import java.util.List;

public final class StageReadinessChecker {
    public ReadinessResult check(CampaignData data, String stageId) {
        Stage stage = data.stage(stageId);
        Order order = data.order(stage.orderId());
        List<String> blockers = new ArrayList<>();
        if (order.isClosed()) {
            blockers.add("Заказ закрыт: передача этапа на согласование недоступна.");
        }
        if (stage.isAccepted()) {
            blockers.add("Этап уже согласован или завершён.");
        }
        List<Task> tasks =
                data.tasks().stream()
                        .filter(task -> task.stageId().equals(stageId) && task.required())
                        .toList();
        if (tasks.isEmpty()) {
            blockers.add("В этапе нет обязательных задач для проверки.");
        }
        List<String> checked = new ArrayList<>();
        List<ReadinessResult.VersionSelection> versions = new ArrayList<>();
        for (Task task : tasks) {
            if (!task.status().equals("done")) {
                blockers.add("Задача " + task.id() + " не завершена: " + task.title() + ".");
            }
            for (TaskMaterial link : data.taskMaterials()) {
                if (!link.taskId().equals(task.id()) || checked.contains(link.materialId())) {
                    continue;
                }
                String materialId = link.materialId();
                checked.add(materialId);
                MaterialVersion version = latestVersion(data.materialVersions(), materialId);
                if (version == null) {
                    blockers.add("У материала " + materialId + " нет ни одной версии.");
                    continue;
                }
                versions.add(new ReadinessResult.VersionSelection(materialId, version.id()));
                if (!version.passedReview()) {
                    blockers.add(
                            "Последняя версия "
                                    + version.id()
                                    + " материала "
                                    + materialId
                                    + " не прошла внутреннюю проверку.");
                }
                List<String> openIds = new ArrayList<>();
                for (Comment comment : data.comments()) {
                    if (comment.versionId().equals(version.id()) && comment.isOpen()) {
                        openIds.add(comment.id());
                    }
                }
                if (!openIds.isEmpty()) {
                    blockers.add(
                            "У версии "
                                    + version.id()
                                    + " есть открытые замечания: "
                                    + String.join(", ", openIds)
                                    + ".");
                }
            }
        }
        if (!tasks.isEmpty() && checked.isEmpty()) {
            blockers.add("Для обязательных задач не указаны материалы результата.");
        }
        return new ReadinessResult(
                stageId,
                stage.orderId(),
                blockers.isEmpty(),
                tasks.size(),
                checked.size(),
                versions,
                blockers);
    }

    private MaterialVersion latestVersion(List<MaterialVersion> versions, String materialId) {
        MaterialVersion latest = null;
        for (MaterialVersion candidate : versions) {
            if (candidate.materialId().equals(materialId)
                    && (latest == null || candidate.number().compareTo(latest.number()) > 0)) {
                latest = candidate;
            }
        }
        return latest;
    }
}
