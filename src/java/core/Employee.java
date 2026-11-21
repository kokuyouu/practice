package core;

public final class Employee {
    private final String id;
    private final String name;
    private final String specialty;

    public Employee(String id, String name, String specialty) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String specialty() {
        return specialty;
    }
}
