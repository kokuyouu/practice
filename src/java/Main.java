import com.google.gson.JsonParseException;

import core.CampaignService;
import core.CampaignDataSource;
import core.ResultPresenter;
import core.Order;
import core.OperationResult;
import core.DeadlineRule;
import core.PositiveBudgetRule;
import core.RequiredTextRule;
import core.BriefValidator;
import core.OverdueTaskFinder;
import core.StageReadinessChecker;
import main.JsonDataSource;
import main.CommandLine;
import main.JsonResultPresenter;
import main.TextResultPresenter;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

public final class Main {
    public static void main(String[] arguments) {
        int exitCode = new Main().run(arguments, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] arguments, PrintStream output, PrintStream errors) {
        try {
            var options = CommandLine.parse(arguments);
            if (options.help()) {
                output.println(CommandLine.usage());
                return 0;
            }
            var jsonSource = new JsonDataSource(options.data());
            CampaignDataSource source = jsonSource;
            var validator =
                    new BriefValidator(
                            List.of(
                                    new RequiredTextRule(Order::goal, "цель кампании"),
                                    new RequiredTextRule(Order::audience, "целевая аудитория"),
                                    new RequiredTextRule(Order::format, "формат кампании"),
                                    new RequiredTextRule(Order::contact, "контакт клиента"),
                                    new PositiveBudgetRule(),
                                    new DeadlineRule()));
            var service =
                    new CampaignService(
                            source.load(),
                            validator,
                            new StageReadinessChecker(),
                            new OverdueTaskFinder());
            OperationResult result =
                    switch (options.command()) {
                        case "brief" -> service.checkBrief(options.order());
                        case "readiness" -> service.checkStageReadiness(options.stage());
                        case "overdue" -> service.findOverdueTasks(options.date(), options.order());
                        case "summary" ->
                                service.buildOrderSummary(options.order(), options.date());
                        case "demo" -> service.demo(options.order(), options.date());
                        default -> throw new IllegalArgumentException("неизвестная команда");
                    };
            ResultPresenter presenter =
                    options.json()
                            ? new JsonResultPresenter(jsonSource.originalData())
                            : new TextResultPresenter();
            output.println(presenter.present(result));
            return 0;
        } catch (IOException | IllegalArgumentException | JsonParseException error) {
            errors.println("Ошибка: " + error.getMessage());
            return 2;
        }
    }
}
