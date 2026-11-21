package core;

import java.util.List;

public record DemoResult(
        BriefResult brief,
        List<ReadinessResult> readiness,
        OverdueResult overdue,
        OrderSummary summary)
        implements OperationResult {
    public DemoResult {
        readiness = List.copyOf(readiness);
    }
}
