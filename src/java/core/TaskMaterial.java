package core;

public final class TaskMaterial {
    private final String taskId;
    private final String materialId;

    public TaskMaterial(String taskId, String materialId) {
        this.taskId = taskId;
        this.materialId = materialId;
    }

    public String taskId() {
        return taskId;
    }

    public String materialId() {
        return materialId;
    }
}
