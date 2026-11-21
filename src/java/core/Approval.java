package core;

import java.time.LocalDate;

public final class Approval {
    private final String id;
    private final String versionId;
    private final String decision;
    private final LocalDate decidedOn;

    public Approval(String id, String versionId, String decision, LocalDate decidedOn) {
        this.id = id;
        this.versionId = versionId;
        this.decision = decision;
        this.decidedOn = decidedOn;
    }

    public String id() {
        return id;
    }

    public String versionId() {
        return versionId;
    }

    public String decision() {
        return decision;
    }

    public LocalDate decidedOn() {
        return decidedOn;
    }
}
