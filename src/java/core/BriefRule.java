package core;


import java.util.List;

public interface BriefRule {
    List<String> validate(Order order);
}
