package core;

public final class OrderMember {
    private final String orderId;
    private final String employeeId;
    private final String projectRole;

    public OrderMember(String orderId, String employeeId, String projectRole) {
        this.orderId = orderId;
        this.employeeId = employeeId;
        this.projectRole = projectRole;
    }

    public String orderId() {
        return orderId;
    }

    public String employeeId() {
        return employeeId;
    }

    public String projectRole() {
        return projectRole;
    }
}
