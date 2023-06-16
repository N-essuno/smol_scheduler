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
    System.out.println(
        "|----------------------------------------| Start run SmolScheduler"
            + " |----------------------------------------|");

    syncAssetModel();

    ARQ.init();
    System.out.println(
        "|--------------------| Start executing SMOL code |--------------------|\n\n");
    execSmol();
    System.out.println(
        "\n\n|--------------------| End executing SMOL code |--------------------|\n\n");
    System.out.println("|--------------------| Start water control |--------------------|\n\n");
    // waterControl();
    System.out.println("\n\n|--------------------| End water control |--------------------|\n\n");
    System.out.println(
        "|----------------------------------------| End run SmolScheduler"
            + " |----------------------------------------|");
  }

  public static void execSmol() {
    REPL repl = new REPL(settings);

    repl.command("verbose", "false");

    repl.command("read", smolPath);
    repl.command("auto", "");

    System.out.println(
        "++++++++++++++++++++++ Start querying lifted state... ++++++++++++++++++++++");
    String needWaterQuery =
        "PREFIX prog: <https://github.com/Edkamb/SemanticObjects/Program#>\n"
            + "SELECT ?plantId "
            + "WHERE { ?plantToWater prog:PlantToWater_plantId ?plantId}";

    ResultSet plantsToWater = repl.getInterpreter().query(needWaterQuery);

    while (plantsToWater.hasNext()) {
      QuerySolution plantToWater = plantsToWater.next();
      // TODO send plantToWater to actuator
      System.out.println(plantToWater);
    }

    System.out.println("++++++++++++++++++++++ End querying lifted state ++++++++++++++++++++++");
    repl.terminate();
  }

  private static void waterControl() {
    // TODO change logic, now not needed to read lifted state, plant to water are given from SPARQL
    // query result
    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(liftedStateOutputFile, ModelTypeEnum.SMOL_MODEL);
    List<Integer> idPlantsToWater = greenhouseModelReader.getPlantsIdsToWater();
    // Close model to free resource access
    greenhouseModelReader.closeModel();
    System.out.println("idPlantsToWater: " + idPlantsToWater);
    startWaterActuator(idPlantsToWater);
  }

  private static void startWaterActuator(List<Integer> idPlantsToWater) {
    if (idPlantsToWater.size() > 0) {
      SshSender sshSender = new SshSender(ConfigTypeEnum.ACTUATOR);
      List<String> cmds = new ArrayList<>();
      cmds.add("cd greenhouse_actuator; python3 -m actuator pump 2");
      sshSender.execCmds(cmds);
    }
  }

  public static void syncAssetModel() {
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    File assetModelFile = new File(greenhouseAssetModelFile);
    long lastModified = assetModelFile.lastModified();
    System.out.println("Asset model current date: " + sdf.format(lastModified));
    System.out.println("Asset model stored date: " + sdf.format(assetModelLastModified));
    if (lastModified != assetModelLastModified) {
      System.out.println("Asset model changed, updating data collector configuration...");
      assetModelLastModified = lastModified;
      updateDataCollectorConfig();
      sendDataCollectorsConfigs();
    }
  }

  private static void updateDataCollectorConfig() {
    INIConfiguration iniConfiguration1 = Utils.readDataCollectorConfig("1");
    INIConfiguration iniConfiguration2 = Utils.readDataCollectorConfig("2");

    GreenhouseModelReader greenhouseModelReader =
        new GreenhouseModelReader(greenhouseAssetModelFile, ModelTypeEnum.ASSET_MODEL);

    List<String> shelf1JsonPots = greenhouseModelReader.getShelfPots("1");
    GreenhouseINIManager.overwriteSection(iniConfiguration1, "pots", "pot", shelf1JsonPots);

    List<String> shelf2JsonPots = greenhouseModelReader.getShelfPots("2");
    GreenhouseINIManager.overwriteSection(iniConfiguration2, "pots", "pot", shelf2JsonPots);

    Utils.writeDataCollectorConfig(iniConfiguration1, "1");
    Utils.writeDataCollectorConfig(iniConfiguration2, "2");
  }

  private static void sendDataCollectorsConfigs() {
    // TODO change to send to data-collector and add sending to both data-collectors
    SshSender sshSender = new SshSender(ConfigTypeEnum.ACTUATOR);
    System.out.println("||||||||||||||||||| Sending data collector configuration ...");
    sshSender.sendFile(localShelf1DataCollectorConfigPath, shelf1DataCollectorConfigPath);
    System.out.println("||||||||||||||||||| Data collector configuration sent");
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
