package core;

import java.time.LocalDate;

public final class ReworkTask extends Task {
    private final String commentId;

    public ReworkTask(
            String id,
            String stageId,
            String employeeId,
            String title,
            LocalDate deadline,
            String status,
            boolean required,
            String commentId) {
        super(id, stageId, employeeId, title, deadline, status, required);
        this.commentId = commentId;
    }

    public String commentId() {
        return commentId;
    }

    @Override
    public void validateContext(CampaignData data) {
        super.validateContext(data);
        Comment comment = data.comment(commentId);
        MaterialVersion version = data.version(comment.versionId());
        Material material = data.material(version.materialId());
        if (!material.stageId().equals(stageId())) {
            throw new IllegalArgumentException("tasks: замечание относится к другому этапу");
        }
    }
}
