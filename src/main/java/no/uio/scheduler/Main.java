package no.uio.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

  public static void main(String[] args) {
    System.out.println("^^^^^^^^^^^^^^^^^^^ Starting Main");

    // check if configs are found, throw an exception otherwise
    checkConfigs();
    System.out.println("^^^^^^^^^^^^^^^^^^^ Configs checked");

    // run the scheduler every interval_seconds seconds
    int intervalSeconds =
        Integer.parseInt(Utils.readSchedulerConfig().get("interval_seconds").toString());
    System.out.println(
        "^^^^^^^^^^^^^^^^^^^ Scheduler interval set to " + intervalSeconds + " seconds ");

    System.out.println("^^^^^^^^^^^^^^^^^^^ Starting scheduled thread");
    ScheduledExecutorService executorService;
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleAtFixedRate(SmolScheduler::run, 0, intervalSeconds, TimeUnit.SECONDS);
  }

  private static void checkConfigs() {
    Utils.readSchedulerConfig();
    Utils.readSshConfig();
  }
}
