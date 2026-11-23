package main;

import core.DateValues;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CommandLine {
    private CommandLine() {}

    public static Options parse(String[] arguments) {
        String command = "demo";
        String order = null;
        String stage = null;
        String asOf = LocalDate.now().toString();
        Path data = null;
        boolean json = false;
        boolean help = false;
        boolean commandSeen = false;
        Set<String> suppliedOptions = new LinkedHashSet<>();
        for (int i = 0; i < arguments.length; i++) {
            String argument = arguments[i];
            if (argument.equals("--json")) {
                json = true;
            } else if (argument.equals("--help") || argument.equals("-h")) {
                help = true;
            } else if (Set.of("--data", "--order", "--stage", "--as-of").contains(argument)) {
                if (++i >= arguments.length || arguments[i].startsWith("--")) {
                    throw new IllegalArgumentException("нужно значение для " + argument);
                }
                String value = arguments[i];
                suppliedOptions.add(argument);
                switch (argument) {
                    case "--data" -> data = Path.of(value);
                    case "--order" -> order = value;
                    case "--stage" -> stage = value;
                    case "--as-of" -> asOf = value;
                    default ->
                            throw new IllegalArgumentException("неизвестный параметр " + argument);
                }
            } else if (!commandSeen
                    && Set.of("brief", "readiness", "overdue", "summary", "demo")
                            .contains(argument)) {
                command = argument;
                commandSeen = true;
            } else {
                throw new IllegalArgumentException("неизвестный аргумент " + argument);
            }
        }
        if (!help) {
            validateOptions(command, suppliedOptions);
            if ((command.equals("brief") || command.equals("summary")) && order == null) {
                throw new IllegalArgumentException("укажите --order");
            }
            if (command.equals("readiness") && stage == null) {
                throw new IllegalArgumentException("укажите --stage");
            }
        }
        if (command.equals("demo") && order == null) {
            order = "O1";
        }
        return new Options(command, order, stage, asOf, data, json, help);
    }

    private static void validateOptions(String command, Set<String> suppliedOptions) {
        Set<String> allowed =
                switch (command) {
                    case "brief" -> Set.of("--order");
                    case "readiness" -> Set.of("--stage");
                    case "overdue", "summary", "demo" -> Set.of("--order", "--as-of");
                    default ->
                            throw new IllegalArgumentException("неизвестная команда " + command);
                };
        for (String option : suppliedOptions) {
            if (!option.equals("--data") && !allowed.contains(option)) {
                throw new IllegalArgumentException(
                        "параметр " + option + " недопустим для команды " + command);
            }
        }
    }

    public static String usage() {
        return """
               Учёт заказов агентства иммерсивного маркетинга
               java -cp "out:lib/gson-2.13.2.jar:data" Main [--data FILE] [--json] COMMAND
                 brief --order ID
                 readiness --stage ID
                 overdue [--order ID] [--as-of YYYY-MM-DD]
                 summary --order ID [--as-of YYYY-MM-DD]
                 demo [--order ID] [--as-of YYYY-MM-DD]
               Без команды запускается demo для O1. Без --data используется data/sample_data.json.
               """
                .stripTrailing();
    }

    public record Options(
            String command,
            String order,
            String stage,
            String asOf,
            Path data,
            boolean json,
            boolean help) {
        public LocalDate date() {
            return DateValues.parse(asOf);
        }
    }
}
