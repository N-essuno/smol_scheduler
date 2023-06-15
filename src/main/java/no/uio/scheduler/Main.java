package no.uio.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;

public class Main {

  public static void main(String[] args) {
    // use -nojar flag to run the program from an IDE, it will use files from src/main/resources
    // directory
    if (args.length > 0 && args[0].equals("-nojar")) {
      Utils.executingJar = false;
    }

    // mainProgram(args);

    // TODO remove test code below after usage and implement config file writing
    System.out.println(
        "-------------------------------------------------------------------------------------");

    INIConfiguration iniConfiguration = Utils.readDataCollectorConfig();
    SubnodeConfiguration pots = iniConfiguration.getSection("pots");
    pots.getKeys()
        .forEachRemaining(
            key -> {
              String jsonPot = pots.getString(key);
              Map<String, Object> potMap = Utils.jsonDictToMap(jsonPot);
              for (Map.Entry<String, Object> entry : potMap.entrySet()) {
                System.out.println(entry.getKey() + " : " + entry.getValue());
              }
            });

    System.out.println("|||||||| SPARQL QUERY ||||||||");
    String greenhouseAssetModelFile =
        Utils.readSchedulerConfig().get("greenhouse_asset_model_file").toString();
    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);
    List<String> jsonPots = greenhouseModelReader.getPots();
    System.out.println(jsonPots);

    System.out.println(
        "-------------------------------------------------------------------------------------");
  }

  private static void mainProgram(String[] args) {
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
