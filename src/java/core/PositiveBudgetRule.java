package core;


import java.math.BigInteger;
import java.util.List;

public final class PositiveBudgetRule implements BriefRule {
    @Override
    public List<String> validate(Order order) {
        Number amount = order.budget();
        boolean valid =
                amount instanceof BigInteger integer
                        ? integer.signum() > 0
                        : amount != null
                                && Double.isFinite(amount.doubleValue())
                                && amount.doubleValue() > 0;
        if (!valid) {
            return List.of("Бюджет должен быть положительным конечным числом.");
        }
        return List.of();
    }
}
