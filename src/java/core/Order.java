package core;

public final class Order {
    private final String id;
    private final String clientId;
    private final String title;
    private final String goal;
    private final String audience;
    private final String format;
    private final String contact;
    private final Number budget;
    private final String deadline;
    private final String status;

    public Order(
            String id,
            String clientId,
            String title,
            String goal,
            String audience,
            String format,
            String contact,
            Number budget,
            String deadline,
            String status) {
        this.id = id;
        this.clientId = clientId;
        this.title = title;
        this.goal = goal;
        this.audience = audience;
        this.format = format;
        this.contact = contact;
        this.budget = budget;
        this.deadline = deadline;
        this.status = status;
    }

    public String id() {
        return id;
    }

    public String clientId() {
        return clientId;
    }

    public String title() {
        return title;
    }

    public String goal() {
        return goal;
    }

    public String audience() {
        return audience;
    }

    public String format() {
        return format;
    }

    public String contact() {
        return contact;
    }

    public Number budget() {
        return budget;
    }

    public String deadline() {
        return deadline;
    }

    public String status() {
        return status;
    }

    public boolean isClosed() {
        return status.equals("completed") || status.equals("cancelled");
    }
}
