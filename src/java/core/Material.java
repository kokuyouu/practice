package core;

public final class Material {
    private final String id;
    private final String stageId;
    private final String name;

    public Material(String id, String stageId, String name) {
        this.id = id;
        this.stageId = stageId;
        this.name = name;
    }

    public String id() {
        return id;
    }

    public String stageId() {
        return stageId;
    }

    public String name() {
        return name;
    }
}
