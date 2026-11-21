package core;


import java.util.List;

public final class DeadlineRule implements BriefRule {
    @Override
    public List<String> validate(Order order) {
        try {
            DateValues.parse(order.deadline());
            return List.of();
        } catch (IllegalArgumentException error) {
            return List.of("Срок сдачи должен быть корректной датой YYYY-MM-DD.");
        }
    }
}
