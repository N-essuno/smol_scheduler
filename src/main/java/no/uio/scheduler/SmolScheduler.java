package no.uio.scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import no.uio.microobject.main.Settings;
import no.uio.microobject.runtime.REPL;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.jena.query.ARQ;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.jetbrains.annotations.NotNull;
import jakarta.jms.JMSException;
import org.json.JSONObject;

public class SmolScheduler {
  private final Utils utils;
  private final GreenhouseINIManager greenhouseINIManager;
  private final String liftedStateOutputFile;
  private final String liftedStateOutputPath;
  private final String greenhouseAssetModelFile;
  private final String domainPrefixUri;
  private final Map<Integer, String[]> shelfCollectorConfigPaths;
  private final String queueUrl;
  private final REPL repl;

  private long assetModelLastModified = 0;

  public SmolScheduler(Utils utils, GreenhouseINIManager greenhouseINIManager) {
    this.utils = utils;
    this.greenhouseINIManager = greenhouseINIManager;

    Map<String, Object> configMap = this.utils.readSchedulerConfig();

    this.liftedStateOutputFile = configMap.get("lifted_state_output_file").toString();
    this.liftedStateOutputPath = configMap.get("lifted_state_output_path").toString();
    this.greenhouseAssetModelFile = configMap.get("greenhouse_asset_model_file").toString();
    this.domainPrefixUri = configMap.get("domain_prefix_uri").toString();

    Settings settings = getSettings();
    String smolPath = configMap.get("smol_path").toString();

    this.repl = new REPL(settings);
    this.repl.command("verbose", "true");
    this.repl.command("read", smolPath);

    Map<String, Object> queueMap = this.utils.readQueueConfig();
    this.queueUrl = queueMap.get("queue_url").toString();

    this.shelfCollectorConfigPaths = new HashMap<>();
    // load all config of shelves
    for (int i=0; i<this.utils.getShelf(); i++) {
      shelfCollectorConfigPaths.put(i+1, new String[]{
              configMap.get("local_shelf_" + (i+1) + "_data_collector_config_path").toString(),
              configMap.get("shelf_" + (i+1) + "_data_collector_config_path").toString()});
    }
  }

  public REPL getRepl() {
    return this.repl;
  }

  public void run() throws JMSException {
    this.utils.printMessage("Start run SmolScheduler", false);

    // start asset model sync thread: check asset model edits and updates configuration
    syncAssetModel();

    ARQ.init();

    this.utils.printMessage("Start executing SMOL code\n", false);
    ResultSet plantsToWater = execSmol();
    this.utils.printMessage("End executing SMOL code", false);

    this.utils.printMessage("Start water control\n", false);
    waterControl(plantsToWater);
    this.utils.printMessage("End water control", false);

    this.utils.printMessage("End run SmolScheduler", false);
  }

  private ResultSet execSmol() {
    //REPL repl = new REPL(settings);

    this.repl.command("auto", "");
      assert this.repl.getInterpreter() != null;
      this.repl.getInterpreter().evalCall(
            this.repl.getInterpreter().getObjectNames("AssetModel").get(0),
            "AssetModel",
            "reconfigure");

    this.utils.printMessage("Start querying lifted state...", true);
    String needWaterQuery =
        "PREFIX prog: <https://github.com/Edkamb/SemanticObjects/Program#>\n"
            + "SELECT ?plantId "
            + "WHERE { ?plantToWater prog:PlantToWater_plantId ?plantId . }";

    assert this.repl.getInterpreter() != null;
    ResultSet plantsToWater = this.repl.getInterpreter().query(needWaterQuery);

    this.utils.printMessage("End querying lifted state", true);

    this.repl.terminate();

    return plantsToWater;
  }

  private void waterControl(ResultSet plantsToWater) throws JMSException {
    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);

    // list of pump pins to activate
    List<Integer> pumpPins = new ArrayList<>();

    while (plantsToWater.hasNext()) {
      QuerySolution plantToWater = plantsToWater.next();
      String plantId = plantToWater.get("?plantId").asLiteral().toString();
      this.utils.printMessage("plantToWater: " + plantId, false);

      // get pump pin which waters plant with id plantId
      int pumpPin = greenhouseModelReader.getPumpPinForPlant(plantId);
      this.utils.printMessage("pumpPin: " + pumpPin, false);
      pumpPins.add(pumpPin);
    }
    this.utils.printMessage("Start watering", false);

    startWaterActuator(pumpPins);
  }

  @NotNull
  private static List<String> getStrings(List<Integer> pumpPinsToActivate) {
    List<String> cmds = new ArrayList<>();
    for (Integer pumpIn : pumpPinsToActivate) {
      cmds.add("[WATER]water " + pumpIn + " 2");
    }
    return cmds;
  }

  private void startWaterActuator(List<Integer> pumpPinsToActivate) throws JMSException {
    this.utils.printMessage("Start water actuator", false);

    if (!pumpPinsToActivate.isEmpty()) {
//      final List<String> cmds = getStrings(pumpPinsToActivate);
//
//      this.utils.printMessage("Water cmds: " + cmds, false);

      // We are not gonna connect to any machines if we are in local
      if (this.utils.getExeuctionMode() == ExecutionModeEnum.REMOTE) {
//        Publisher publisher = new Publisher(queueUrl);
//
//        try {
//          publisher.publish("actuator.1.water", cmds.toString());
//        } catch (Exception e) {
//          e.printStackTrace();
//        }
        for (Integer pumpPin : pumpPinsToActivate) {
          Publisher publisher = new Publisher(queueUrl);

          try {
            String command =  "[WATER]" + String.valueOf(pumpPin) + " 2";
            this.utils.printMessage("Water cmd: " + command, false);
            publisher.publish("actuator.1.water", command);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }
  }

  public void syncAssetModel() {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    File assetModelFile = new File(greenhouseAssetModelFile);
    long lastModified = assetModelFile.lastModified();
    this.utils.printMessage("Asset model current date: " + sdf.format(lastModified), false);
    this.utils.printMessage("Asset model stored date: " + sdf.format(assetModelLastModified), false);

    // check if asset model has been modified, then update data collector configuration and send it
    if (lastModified > assetModelLastModified) {
      this.utils.printMessage("Asset model changed, updating data collector configuration...", false);
      assetModelLastModified = lastModified;
      updateDataCollectorsConfig();
    }
  }

  private void updateDataCollectorsConfig() {
    for (int i=0; i<this.utils.getShelf(); i++) {
      // read data collectors configurations from files
      String shelfFloor = String.valueOf(i + 1); // We start from 0, but pots starts from 1

      INIConfiguration iniConfiguration = this.utils.readDataCollectorConfig(shelfFloor);
      GreenhouseModelReader greenhouseModelReader =
              new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);

      // get shelves from asset model, including Raspberry connection mapping.
      // Shelves are stored as JSON strings
      List<String> shelfJson = greenhouseModelReader.getShelf(shelfFloor);
      // get pots on shelf from asset model, including Raspberry connection mapping.
      // Pots are stored as JSON strings
      List<String> shelfJsonPot = greenhouseModelReader.getShelfPots(shelfFloor);
      // get plants on shelf from asset model.
      // Plants are stored as JSON strings
      List<String> shelfJsonPlants = greenhouseModelReader.getShelfPlants(shelfFloor);

      // overwrite shelves section in data collector INI configuration files
      this.greenhouseINIManager.overwriteSection(iniConfiguration, "shelves", "shelf", shelfJson);
      // overwrite pots section in data collector INI configuration files
      this.greenhouseINIManager.overwriteSection(iniConfiguration, "pots", "pot", shelfJsonPot);
      // overwrite plants section in data collector INI configuration files
      this.greenhouseINIManager.overwriteSection(iniConfiguration, "plants", "plant", shelfJsonPlants);

      // write data collector configuration files
      this.utils.writeDataCollectorConfig(iniConfiguration, shelfFloor);

      String updatedConfig = this.utils.readDataCollectorConfigJson(shelfFloor).toString();

      if (updatedConfig != null) {
        // Prepend the [CONFIG] token to the configuration
        changeDataCollectorsConfigs(shelfFloor, "[CONFIG]" + updatedConfig);
      }
    }
  }

  private void changeDataCollectorsConfigs(String id, String command) {
    if(this.utils.getExeuctionMode() == ExecutionModeEnum.LOCAL) {
      return;
    }

    try {
      this.utils.printMessage("Sending data collector configuration for shelf " + id + "..." + command, false);
      Publisher publisher = new Publisher(queueUrl);
      publisher.publish("collector." + id + ".config.change", command);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @NotNull
  private Settings getSettings() {
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
    assetModel = ""; // TODO: remove the asset model at once when it's done
    String tripleStoreUrl = "http://localhost:3030/GreenHouse";
    String reasoner = "off"; // we don't want the reasoner

    return new Settings(
        verbose,
        materialize,
        kgOutput,
        assetModel,
        tripleStoreUrl,
        domainPrefix,
        progPrefix,
        runPrefix,
        langPrefix,
        extraPrefixes,
        useQueryType,
        reasoner);
  }

  private String getAssetModel(String assetModel) {
    // Read the asset model from the file
    try {
      return Files.readString(new File(assetModel).toPath());
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
}
