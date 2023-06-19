package no.uio.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.yaml.snakeyaml.Yaml;

public class Utils {

  public static boolean executingJar = true;
  private static final String currentPath;

  // Used to get current working directory
  static {
    try {
      currentPath =
          new File(
                  Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
              .getParent();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static Path schedulerConfigPath = Path.of(currentPath).resolve("config_scheduler.yml");
  private static Path sshConfigPath = Path.of(currentPath).resolve("config_ssh.yml");
  private static Path shelf1DataCollectorConfigPath =
      Path.of(currentPath).resolve("config_shelf_1.ini");
  private static Path shelf2DataCollectorConfigPath =
      Path.of(currentPath).resolve("config_shelf_2.ini");

  /** Read configuration files formatted as YAML and return a key-value Map. */
  public static Map<String, Object> readConfig(String configPath) {
    InputStream inputStream;
    try {
      inputStream = new FileInputStream(configPath);
    } catch (FileNotFoundException e) {
      System.out.println("ERROR: Config file not found at " + configPath);
      throw new RuntimeException(e);
    }

    Yaml yaml = new Yaml();
    Map<String, Object> configMap = yaml.load(inputStream);

    try {
      inputStream.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    return configMap;
  }

  public static Map<String, Object> readSchedulerConfig() {
    if (executingJar) {
      return readConfig(schedulerConfigPath.toString());
    } else {
      return readConfig("src/main/resources/config_scheduler.yml");
    }
  }

  public static Map<String, Object> readSshConfig() {
    if (executingJar) {
      return readConfig(sshConfigPath.toString());
    } else {
      return readConfig("src/main/resources/config_ssh.yml");
    }
  }

  // TODO Make this and the config file setup more generic in order to work with any shelf
  public static INIConfiguration readDataCollectorConfig(String shelfFloor) {
    String path;
    if (executingJar) {
      path =
          switch (shelfFloor) {
            case "1" -> shelf1DataCollectorConfigPath.toString();
            case "2" -> shelf2DataCollectorConfigPath.toString();
            default -> throw new RuntimeException("Invalid shelf floor: " + shelfFloor);
          };
    } else {
      path =
          switch (shelfFloor) {
            case "1" -> "src/main/resources/config_shelf_1.ini";
            case "2" -> "src/main/resources/config_shelf_2.ini";
            default -> throw new RuntimeException("Invalid shelf floor: " + shelfFloor);
          };
    }

    INIConfiguration iniConfiguration = new INIConfiguration();
    try (FileReader fileReader = new FileReader(path)) {
      iniConfiguration.read(fileReader);
    } catch (IOException | ConfigurationException e) {
      throw new RuntimeException(e);
    }

    return iniConfiguration;
  }

  // TODO Improvement: make this and the config file setup more generic in order to work with any
  // shelf
  public static void writeDataCollectorConfig(
      INIConfiguration iniConfiguration, String shelfFloor) {
    String path;
    if (executingJar) {
      path =
          switch (shelfFloor) {
            case "1" -> shelf1DataCollectorConfigPath.toString();
            case "2" -> shelf2DataCollectorConfigPath.toString();
            default -> throw new RuntimeException("Invalid shelf floor: " + shelfFloor);
          };
    } else {
      path =
          switch (shelfFloor) {
            case "1" -> "src/main/resources/config_shelf_1.ini";
            case "2" -> "src/main/resources/config_shelf_2.ini";
            default -> throw new RuntimeException("Invalid shelf floor: " + shelfFloor);
          };
    }

    try {
      iniConfiguration.write(new FileWriter(path));
    } catch (ConfigurationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, Object> jsonDictToMap(String jsonDict) {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> map;
    try {
      map = mapper.readValue(jsonDict, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }

    return map;
  }

  public static void printMessage(String message, boolean runningSmol) {
    if (runningSmol) {
      System.out.println("SMOL-EXEC> " + message);
    } else {
      System.out.println("SCHEDULER-OUT> " + message);
    }
  }

  // TODO: decide if needed
  //  public static Map<String, Object> readInfluxConfig() {
  //    if (executingJar) {
  //      Path path = Path.of(currentPath).resolve("config_ssh.yml");
  //      return readConfig(path.toString());
  //    } else {
  //      return readConfig("src/main/resources/config_local.yml");
  //    }
  //  }
}
