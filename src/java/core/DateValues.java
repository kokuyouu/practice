package core;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class DateValues {
    private DateValues() {}

    public static LocalDate parse(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            throw new IllegalArgumentException("дата должна иметь формат YYYY-MM-DD");
        }
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.getYear() < 1) {
                throw new IllegalArgumentException(
                        "дата должна быть существующей датой YYYY-MM-DD");
            }
            return date;
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException(
                    "дата должна быть существующей датой YYYY-MM-DD", error);
        }
    }
}
