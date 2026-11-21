package core;

public final class Comment {
    private final String id;
    private final String versionId;
    private final String text;
    private final String status;

    public Comment(String id, String versionId, String text, String status) {
        this.id = id;
        this.versionId = versionId;
        this.text = text;
        this.status = status;
    }

    public String id() {
        return id;
    }

    public String versionId() {
        return versionId;
    }

    public String text() {
        return text;
    }

    public String status() {
        return status;
    }

    public boolean isOpen() {
        return status.equals("open");
    }
}
