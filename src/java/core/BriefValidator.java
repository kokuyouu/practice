package core;


import java.util.ArrayList;
import java.util.List;

public final class BriefValidator {
    private final List<BriefRule> rules;

    public BriefValidator(List<BriefRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public BriefResult check(Order order) {
        List<String> errors = new ArrayList<>();
        for (BriefRule rule : rules) {
            errors.addAll(rule.validate(order));
        }
        return new BriefResult(order.id(), errors.isEmpty(), errors);
    }
}
