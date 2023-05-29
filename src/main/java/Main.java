
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("-nojar")) {
            Utils.executingJar = false;
        }

        int intervalSeconds = Integer
            .parseInt(
                Utils.readSchedulerConfig()
                .get("interval_seconds")
                .toString()
            );

        ScheduledExecutorService executorService;
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(SmolScheduler::run, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    private static void checkConfigs() {
        Utils.readSchedulerConfig();
        Utils.readSshConfig();
    }
}
