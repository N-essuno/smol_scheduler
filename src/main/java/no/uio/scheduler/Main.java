package no.uio.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.jms.JMSException;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.jena.query.ARQ;

public class Main {

  public static void main(String[] args) {
    Utils utils;

    if (args.length > 0 && args[0].equals("d")) {
      utils = new Utils(ExecutionModeEnum.LOCAL);
    } else {
      utils = new Utils(ExecutionModeEnum.REMOTE);
    }

    GreenhouseINIManager greenhouseINIManager = new GreenhouseINIManager(utils);
    SmolScheduler smolScheduler = new SmolScheduler(utils, greenhouseINIManager);

    // Execute the Subscribe into a separate thread
    Thread subscriberStoreThread = new Thread(() -> {
      try {
        Subscriber subscriber = new Subscriber(utils.readQueueConfig().get("broker_url").toString(), smolScheduler);
//        subscriber.subscribe(utils.readQueueConfig().get("queue_name").toString());
        subscriber.subscribe("controller.1.asset.model");

        Thread.sleep(Long.MAX_VALUE);
      } catch (JMSException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    subscriberStoreThread.start();

    Thread subscriberTimeThread = new Thread(() -> {
      try {
        Subscriber subscriber = new Subscriber(utils.readQueueConfig().get("broker_url").toString(), smolScheduler);
//        subscriber.subscribe(utils.readQueueConfig().get("queue_name").toString());
        subscriber.subscribe("controller.1.exec.time");

        Thread.sleep(Long.MAX_VALUE);
      } catch (JMSException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    subscriberTimeThread.start();

    // use -nojar flag to run the program from an IDE, it will use files from src/main/resources
    // directory
    if (args.length > 0 && args[0].equals("-nojar")) {
      utils.executingJar = false;
    } else if (args.length > 0 && args[0].equals("d")) {
      testProgram(utils, greenhouseINIManager, smolScheduler);
    } else {
      mainProgram(utils, greenhouseINIManager, smolScheduler);
    }
  }

  private static void testProgram(Utils utils, GreenhouseINIManager greenhouseINIManager, SmolScheduler smolScheduler) {
    ARQ.init();
    utils.printMessage("Starting Test", false);

    // check if configs are found, throw an exception otherwise
    checkConfigs(utils);
    utils.printMessage("Configs checked", false);

    System.out.println("\n\n|||||||| SPARQL QUERY ||||||||");
    String greenhouseAssetModelFile =
            utils.readSchedulerConfig().get("greenhouse_asset_model_file").toString();

    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);

    INIConfiguration iniConfiguration = utils.readDataCollectorConfig("1");
    INIConfiguration iniConfiguration2 = utils.readDataCollectorConfig("2");

    List<String> shelf1JsonPots = greenhouseModelReader.getShelfPots("1");
    System.out.println("Shelf1 Json Pots: " + shelf1JsonPots);
    greenhouseINIManager.overwriteSection(iniConfiguration, "pots", "pot", shelf1JsonPots);

    System.out.println("*********************** CONFIG 1");
    greenhouseINIManager.printFile(iniConfiguration);

    List<String> shelf2JsonPots = greenhouseModelReader.getShelfPots("2");
    System.out.println("Shelf2 Json Pots: " + shelf2JsonPots);
    greenhouseINIManager.overwriteSection(iniConfiguration2, "pots", "pot", shelf2JsonPots);

    System.out.println("*********************** CONFIG 2");
    greenhouseINIManager.printFile(iniConfiguration2);

    smolScheduler.syncAssetModel();

    utils.writeDataCollectorConfig(iniConfiguration, "1");
    utils.writeDataCollectorConfig(iniConfiguration2, "2");

    // run the scheduler every interval_seconds seconds
    int intervalSeconds =
            Integer.parseInt(utils.readSchedulerConfig().get("interval_seconds").toString());
    utils.printMessage("Scheduler interval set to " + intervalSeconds + " seconds ", false);

    // start SMOL scheduler
    utils.printMessage("Starting scheduled thread", false);
    ScheduledExecutorService executorService;
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleAtFixedRate(() -> {
      try {
        smolScheduler.run();
      } catch (JMSException e) {
        // Handle the exception (e.g., log it or take appropriate action)
      }
    }, 0, intervalSeconds, TimeUnit.SECONDS);
  }

  private static void mainProgram(Utils utils, GreenhouseINIManager greenhouseINIManager, SmolScheduler smolScheduler) {
    ARQ.init();
    utils.printMessage("Starting Main", false);

    // check if configs are found, throw an exception otherwise
    checkConfigs(utils);
    utils.printMessage("Configs checked", false);

    // run the scheduler every interval_seconds seconds
    int intervalSeconds = smolScheduler.getExecutionTime();
    utils.printMessage("Scheduler interval set to " + intervalSeconds + " seconds ", false);

    // do first sync of configuration from asset model
    utils.printMessage("Starting data-collectors", false);
    smolScheduler.syncAssetModel();

    // start SMOL scheduler
    utils.printMessage("Starting scheduled thread", false);
    ScheduledExecutorService executorService;
    executorService = Executors.newSingleThreadScheduledExecutor();
    executorService.scheduleAtFixedRate(() -> {
      try {
        smolScheduler.run();
      } catch (JMSException e) {
        // Handle the exception (e.g., log it or take appropriate action)
      }
    }, 0, intervalSeconds, TimeUnit.SECONDS);
  }

  private static void checkConfigs(Utils utils) {
    utils.readSchedulerConfig();
    utils.readSshConfig();
    utils.readQueueConfig();
  }
}
