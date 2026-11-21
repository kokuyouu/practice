package core;

public final class Stage {
    private final String id;
    private final String orderId;
    private final String name;
    private final String status;

    public Stage(String id, String orderId, String name, String status) {
        this.id = id;
        this.orderId = orderId;
        this.name = name;
        this.status = status;
    }

    public String id() {
        return id;
    }

    public String orderId() {
        return orderId;
    }

    public String name() {
        return name;
    }

    public String status() {
        return status;
    }

    public boolean isAccepted() {
        return status.equals("approved") || status.equals("completed");
    }
}
