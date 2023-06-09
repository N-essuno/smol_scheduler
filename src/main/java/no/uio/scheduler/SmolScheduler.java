package no.uio.scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.uio.microobject.main.Settings;
import no.uio.microobject.runtime.REPL;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.jetbrains.annotations.NotNull;

public class SmolScheduler {
  private static final Map<String, Object> configMap = Utils.readSchedulerConfig();
  private static final String liftedStateOutputFile =
      configMap.get("lifted_state_output_file").toString();
  private static final String liftedStateOutputPath =
      configMap.get("lifted_state_output_path").toString();
  private static final String greenhouseAssetModelFile =
      configMap.get("greenhouse_asset_model_file").toString();
  private static final String domainPrefixUri = configMap.get("domain_prefix_uri").toString();
  private static final Settings settings = getSettings();
  private static final String smolPath = configMap.get("smol_path").toString();
  private static final String shelf1DataCollectorConfigPath =
      configMap.get("shelf_1_data_collector_config_path").toString();
  private static final String shelf2DataCollectorConfigPath =
      configMap.get("shelf_2_data_collector_config_path").toString();
  private static final String localShelf1DataCollectorConfigPath =
      configMap.get("local_shelf_1_data_collector_config_path").toString();
  private static final String localShelf2DataCollectorConfigPath =
      configMap.get("local_shelf_2_data_collector_config_path").toString();

  private static long assetModelLastModified = 0;

  public static void run() {
    Utils.printMessage("Start run SmolScheduler", false);

    // start asset model sync thread: check asset model edits and updates configuration
    syncAssetModel();

    ARQ.init();

    Utils.printMessage("Start executing SMOL code\n", false);
    ResultSet plantsToWater = execSmol();
    Utils.printMessage("End executing SMOL code", false);

    Utils.printMessage("Start water control\n", false);
    waterControl(plantsToWater);
    Utils.printMessage("End water control", false);

    Utils.printMessage("End run SmolScheduler", false);
  }

  private static ResultSet execSmol() {
    REPL repl = new REPL(settings);

    repl.command("verbose", "true");

    repl.command("read", smolPath);
    repl.command("auto", "");

    Utils.printMessage("Start querying lifted state...", true);
    String needWaterQuery =
        "PREFIX prog: <https://github.com/Edkamb/SemanticObjects/Program#>\n"
            + "SELECT ?plantId "
            + "WHERE { ?plantToWater prog:PlantToWater_plantId ?plantId}";

    ResultSet plantsToWater = repl.getInterpreter().query(needWaterQuery);

    Utils.printMessage("End querying lifted state", true);

    repl.terminate();

    return plantsToWater;
  }

  private static void waterControl(ResultSet plantsToWater) {
    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);

    // list of pump pins to activate
    List<Integer> pumpPins = new ArrayList<>();

    while (plantsToWater.hasNext()) {
      QuerySolution plantToWater = plantsToWater.next();
      String plantId = plantToWater.get("?plantId").asLiteral().toString();
      System.out.println("plantToWater: " + plantId);

      // get pump pin which waters plant with id plantId
      int pumpPin = greenhouseModelReader.getPumpPinForPlant(plantId);
      System.out.println("pumpPin: " + pumpPin);
      pumpPins.add(pumpPin);
    }
    startWaterActuator(pumpPins);
  }

  private static void startWaterActuator(List<Integer> pumpPinsToActivate) {
    Utils.printMessage("Start water actuator", false);

    if (pumpPinsToActivate.size() > 0) {
      SshSender sshSender = new SshSender(ConfigTypeEnum.ACTUATOR);
      List<String> cmds = new ArrayList<>();
      for (Integer pumpPin : pumpPinsToActivate) {
        // activate pump connected to pin pumpPin for 2 seconds
        cmds.add("cd greenhouse_actuator; python3 -m actuator water " + pumpPin + " 2");
      }
      System.out.println("water cmds: " + cmds);
      sshSender.execCmds(cmds);
    }
  }

  public static void syncAssetModel() {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    File assetModelFile = new File(greenhouseAssetModelFile);
    long lastModified = assetModelFile.lastModified();
    Utils.printMessage("Asset model current date: " + sdf.format(lastModified), false);
    Utils.printMessage("Asset model stored date: " + sdf.format(assetModelLastModified), false);

    // check if asset model has been modified, then update data collector configuration and send it
    if (lastModified > assetModelLastModified) {
      Utils.printMessage("Asset model changed, updating data collector configuration...", false);
      assetModelLastModified = lastModified;
      updateDataCollectorsConfig();
      sendDataCollectorsConfigs();
    }
  }

  private static void updateDataCollectorsConfig() {
    // read data collectors configurations from files
    INIConfiguration iniConfiguration1 = Utils.readDataCollectorConfig("1");
    INIConfiguration iniConfiguration2 = Utils.readDataCollectorConfig("2");

    // create model reader to read asset model
    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);

    // get shelves 1 and 2 from asset model, including Raspberry connection mapping.
    // Shelves are stored as JSON strings
    List<String> shelf1Json = greenhouseModelReader.getShelf("1");
    List<String> shelf2Json = greenhouseModelReader.getShelf("2");

    // get pots on shelf 1 and 2 from asset model, including Raspberry connection mapping.
    // Pots are stored as JSON strings
    List<String> shelf1JsonPots = greenhouseModelReader.getShelfPots("1");
    List<String> shelf2JsonPots = greenhouseModelReader.getShelfPots("2");

    // get plants on shelf 1 and 2 from asset model.
    // Plants are stored as JSON strings
    List<String> shelf1JsonPlants = greenhouseModelReader.getShelfPlants("1");
    List<String> shelf2JsonPlants = greenhouseModelReader.getShelfPlants("2");

    // overwrite shelves section in data collector INI configuration files
    GreenhouseINIManager.overwriteSection(iniConfiguration1, "shelves", "shelf", shelf1Json);
    GreenhouseINIManager.overwriteSection(iniConfiguration2, "shelves", "shelf", shelf2Json);

    // overwrite pots section in data collector INI configuration files
    GreenhouseINIManager.overwriteSection(iniConfiguration1, "pots", "pot", shelf1JsonPots);
    GreenhouseINIManager.overwriteSection(iniConfiguration2, "pots", "pot", shelf2JsonPots);

    // overwrite plants section in data collector INI configuration files
    GreenhouseINIManager.overwriteSection(iniConfiguration1, "plants", "plant", shelf1JsonPlants);
    GreenhouseINIManager.overwriteSection(iniConfiguration2, "plants", "plant", shelf2JsonPlants);

    // write data collector configuration files
    Utils.writeDataCollectorConfig(iniConfiguration1, "1");
    Utils.writeDataCollectorConfig(iniConfiguration2, "2");
  }

  private static void sendDataCollectorsConfigs() {
    SshSender sshSender = new SshSender(ConfigTypeEnum.DATA_COLLECTOR_1);

    Utils.printMessage("Sending data collector configuration...", false);
    // send local data collector configuration files to remote data-collectors
    sshSender.sendFile(localShelf1DataCollectorConfigPath, shelf1DataCollectorConfigPath);

    //    sshSender.setConfig(ConfigTypeEnum.DATA_COLLECTOR_2);
    //    sshSender.sendFile(localShelf2DataCollectorConfigPath, shelf2DataCollectorConfigPath);
    Utils.printMessage("Data collector configuration sent", false);
  }

  @NotNull
  private static Settings getSettings() {
    boolean verbose = true;
    boolean materialize = false;
    String kgOutput = liftedStateOutputPath;
    String greenhouseAssetModel = greenhouseAssetModelFile;
    String domainPrefix = domainPrefixUri;
    String progPrefix = "https://github.com/Edkamb/SemanticObjects/Program#";
    String runPrefix =
        "https://github.com/Edkamb/SemanticObjects/Run" + System.currentTimeMillis() + "#";
    String langPrefix = "https://github.com/Edkamb/SemanticObjects#";
    HashMap<String, String> extraPrefixes = new HashMap<>();
    boolean useQueryType = false;

    String assetModel = getAssetModel(greenhouseAssetModel);

    return new Settings(
        verbose,
        materialize,
        kgOutput,
        assetModel,
        domainPrefix,
        progPrefix,
        runPrefix,
        langPrefix,
        extraPrefixes,
        useQueryType);
  }

  private static String getAssetModel(String assetModel) {
    // Read the asset model from the file
    try {
      return Files.readString(new File(assetModel).toPath());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
