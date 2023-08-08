package no.uio.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.jena.query.ARQ;

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
    ARQ.init();
    Utils.printMessage("Starting Main", false);

    // check if configs are found, throw an exception otherwise
    checkConfigs();
    Utils.printMessage("Configs checked", false);

    // run the scheduler every interval_seconds seconds
    int intervalSeconds =
        Integer.parseInt(Utils.readSchedulerConfig().get("interval_seconds").toString());
    Utils.printMessage("Scheduler interval set to " + intervalSeconds + " seconds ", false);

    // do first sync of configuration from asset model
    Utils.printMessage("Starting data-collectors", false);
    SmolScheduler.syncAssetModel();

    // start data collectors
    startDataCollectors();

    // start SMOL scheduler
    Utils.printMessage("Starting scheduled thread", false);
    ScheduledExecutorService executorService;
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleAtFixedRate(SmolScheduler::run, 0, intervalSeconds, TimeUnit.SECONDS);
  }

  private static void startDataCollectors() {
    // TODO change: note - starting only shelf 1 data collector now and starting in demo mode
    SshSender sshSender = new SshSender(ConfigTypeEnum.DATA_COLLECTOR_1);
    List<String> cmds = new ArrayList<>();
    cmds.add(
        "nohup bash -c "
            + "'cd influx_greenhouse/greenhouse-data-collector; python3 -m collector' "
            + ">/dev/null "
            + "2>/dev/null &");

    // exec start command for data collectors of shelf 1 and 2
    sshSender.execCmds(cmds);

    //    sshSender.setConfig(ConfigTypeEnum.DATA_COLLECTOR_2);
    //    sshSender.execCmds(cmds);
  }

  private static void checkConfigs() {
    Utils.readSchedulerConfig();
    Utils.readSshConfig();
  }
}
