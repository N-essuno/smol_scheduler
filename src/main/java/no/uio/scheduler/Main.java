package no.uio.scheduler;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration2.INIConfiguration;

public class Main {

  public static void main(String[] args) {
    // use -nojar flag to run the program from an IDE, it will use files from src/main/resources
    // directory
    if (args.length > 0 && args[0].equals("-nojar")) {
      Utils.executingJar = false;
    }

    mainProgram();
    // randomTest();

  }

  public static void randomTest() {
    // TODO remove test code below after usage and implement config file writing
    System.out.println(
        "-------------------------------------------------------------------------------------");

    INIConfiguration iniConfiguration1 = Utils.readDataCollectorConfig("1");
    INIConfiguration iniConfiguration2 = Utils.readDataCollectorConfig("2");

    System.out.println("\n\n|||||||| SPARQL QUERY ||||||||");
    String greenhouseAssetModelFile =
        Utils.readSchedulerConfig().get("greenhouse_asset_model_file").toString();

    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);

    List<String> shelf1JsonPots = greenhouseModelReader.getShelfPots("1");
    System.out.println("Shelf1 Json Pots: " + shelf1JsonPots);
    GreenhouseINIManager.overwriteSection(iniConfiguration1, "pots", "pot", shelf1JsonPots);

    System.out.println("*********************** CONFIG 1");
    GreenhouseINIManager.printFile(iniConfiguration1);

    List<String> shelf2JsonPots = greenhouseModelReader.getShelfPots("2");
    System.out.println("Shelf2 Json Pots: " + shelf2JsonPots);
    GreenhouseINIManager.overwriteSection(iniConfiguration2, "pots", "pot", shelf2JsonPots);

    System.out.println("*********************** CONFIG 2");
    GreenhouseINIManager.printFile(iniConfiguration2);

    SmolScheduler.syncAssetModel();

    Utils.writeDataCollectorConfig(iniConfiguration1, "1");
    Utils.writeDataCollectorConfig(iniConfiguration2, "2");

    System.out.println(
        "-------------------------------------------------------------------------------------");
  }

  private static void mainProgram() {
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
