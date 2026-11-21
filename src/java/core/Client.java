package core;

public final class Client {
    private final String id;
    private final String name;
    private final String contact;

    public Client(String id, String name, String contact) {
        this.id = id;
        this.name = name;
        this.contact = contact;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String contact() {
        return contact;
    }
}
