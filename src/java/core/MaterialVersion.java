package core;

import java.math.BigInteger;

public final class MaterialVersion {
    private final String id;
    private final String materialId;
    private final BigInteger number;
    private final String url;
    private final String reviewStatus;

    public MaterialVersion(
            String id, String materialId, BigInteger number, String url, String reviewStatus) {
        this.id = id;
        this.materialId = materialId;
        this.number = number;
        this.url = url;
        this.reviewStatus = reviewStatus;
    }

    public String id() {
        return id;
    }

    public String materialId() {
        return materialId;
    }

    public BigInteger number() {
        return number;
    }

    public String url() {
        return url;
    }

    public String reviewStatus() {
        return reviewStatus;
    }

    public boolean passedReview() {
        return reviewStatus.equals("passed");
    }
}
