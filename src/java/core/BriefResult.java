package core;

import java.util.List;

public record BriefResult(String orderId, boolean complete, List<String> errors)
        implements OperationResult {
    public BriefResult {
        errors = List.copyOf(errors);
    }
}
