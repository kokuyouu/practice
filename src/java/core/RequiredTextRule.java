package core;


import java.util.List;
import java.util.function.Function;

public final class RequiredTextRule implements BriefRule {
    private final Function<Order, String> field;
    private final String label;

    public RequiredTextRule(Function<Order, String> field, String label) {
        this.field = field;
        this.label = label;
    }

    @Override
    public List<String> validate(Order order) {
        String value = field.apply(order);
        if (value == null || TextValues.isBlank(value)) {
            return List.of("Не заполнено поле: " + label + ".");
        }
        return List.of();
    }
}
