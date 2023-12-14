package org.smolang.greenhouse.scheduler;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.yaml.snakeyaml.Yaml;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.json.JSONObject;

public class Utils {

  public boolean executingJar = true;
  private final Path schedulerConfigPath;
  private final Path sshConfigPath;
  private final Path queueConfigPath;
  private final HashMap<Integer, Path> shelfDataCollectorConfigPaths = new HashMap<>();
  private int shelf;
  private final ExecutionModeEnum executionMode;

  public Utils(ExecutionModeEnum executionMode) {
    this.executionMode = executionMode;

    String currentPath;
    try {
      currentPath =
              new File(
                      Utils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath())
                      .getParent();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    final Path path = Path.of(currentPath);

    this.sshConfigPath = path.resolve("config_ssh.yml");
    this.queueConfigPath = path.resolve("config_queue.yml");
    this.schedulerConfigPath = path.resolve("config_scheduler.yml");

    this.shelf = 0;
    File[] files = new File(path.resolve("config_shelf").toString()).listFiles();

    assert files != null;
    for (File file : files) {
      if (file.isFile()) {
        if (!file.getName().startsWith(".")) {
          // get the number from filename assuming it is name_long_number.ini
          String[] filename = file.getName().split("_");
          int number = Integer.parseInt(filename[filename.length - 1].split("\\.")[0]);
          shelfDataCollectorConfigPaths.put(number, path.resolve("config_shelf").resolve(file.getName()));
          this.shelf += 1;
        }
      }
    }
  }

  public int getShelf () {
    return this.shelf;
  }

  public ExecutionModeEnum getExecutionMode() {
    return this.executionMode;
  }

  /** Read configuration files formatted as YAML and return a key-value Map. */
  public Map<String, Object> readConfig(String configPath) {
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

  public Map<String, Object> readSchedulerConfig() {
    if (executingJar) {
      return readConfig(schedulerConfigPath.toString());
    } else {
      return readConfig("src/main/resources/config_scheduler.yml");
    }
  }

  public void readSshConfig() {
    if (executingJar) {
      readConfig(sshConfigPath.toString());
    } else {
      readConfig("src/main/resources/config_ssh.yml");
    }
  }

  public Map<String, Object> readQueueConfig() {
    if (executingJar) {
      return readConfig(queueConfigPath.toString());
    } else {
      return readConfig("src/main/resources/config_queue.yml");
    }
  }

  public INIConfiguration readDataCollectorConfig(String shelfFloor) {
    if (!shelfDataCollectorConfigPaths.containsKey(Integer.parseInt(shelfFloor))) {
      throw new RuntimeException("Invalid shelf floor: " + shelfFloor);
    }

    String path;

    if (executingJar) {
      path = shelfDataCollectorConfigPaths.get(Integer.parseInt(shelfFloor)).toString();
    } else {
      path = "src/main/resources/config_shelf_" + shelfFloor + ".ini";
    }

    INIConfiguration iniConfiguration = new INIConfiguration();
    try (FileReader fileReader = new FileReader(path)) {
      iniConfiguration.read(fileReader);
    } catch (IOException | ConfigurationException e) {
      throw new RuntimeException(e);
    }

    return iniConfiguration;
  }

  public JSONObject readDataCollectorConfigJson(String shelfFloor) {
    if (!shelfDataCollectorConfigPaths.containsKey(Integer.parseInt(shelfFloor))) {
      throw new RuntimeException("Invalid shelf floor: " + shelfFloor);
    }
    String path;

    if (executingJar) {
      path = shelfDataCollectorConfigPaths.get(Integer.parseInt(shelfFloor)).toString();
    } else {
      path = "src/main/resources/config_shelf_" + shelfFloor + ".ini";
    }

    System.out.println(path);

    try {
      // Load the .ini file
      Ini ini = new Ini(new FileReader(path));

      // Create a JSON object to store the converted data
      JSONObject json = new JSONObject();

      // Iterate through sections in the .ini file
      for (String sectionName : ini.keySet()) {
        Profile.Section section = ini.get(sectionName);

        // Create a JSON object for each section
        JSONObject sectionJson = new JSONObject();

        // Iterate through key-value pairs in the section
        for (String key : section.keySet()) {
          String value = section.get(key);
          sectionJson.put(key, value);
        }

        // Add the section JSON to the main JSON object
        json.put(sectionName, sectionJson);
      }

      return json;
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public void writeDataCollectorConfig(
      INIConfiguration iniConfiguration, String shelfFloor) {
    if (!shelfDataCollectorConfigPaths.containsKey(Integer.parseInt(shelfFloor))) {
      throw new RuntimeException("Invalid shelf floor: " + shelfFloor);
    }

    String path;

    if (executingJar) {
      path = shelfDataCollectorConfigPaths.get(Integer.parseInt(shelfFloor)).toString();
    } else {
      path = "src/main/resources/config_shelf_" + shelfFloor + ".ini";
    }

    try {
      iniConfiguration.write(new FileWriter(path));
    } catch (ConfigurationException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void printMessage(String message, boolean runningSmol) {
    if (runningSmol) {
      System.out.println("SMOL-EXEC> " + message);
    } else {
      System.out.println("SCHEDULER-OUT> " + message);
    }
  }
}
