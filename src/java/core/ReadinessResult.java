package core;

import java.util.List;

public record ReadinessResult(
        String stageId,
        String orderId,
        boolean ready,
        int requiredTasks,
        int checkedMaterials,
        List<VersionSelection> versions,
        List<String> blockers)
        implements OperationResult {
    public ReadinessResult {
        versions = List.copyOf(versions);
        blockers = List.copyOf(blockers);
    }

    public record VersionSelection(String materialId, String versionId) {}
}
