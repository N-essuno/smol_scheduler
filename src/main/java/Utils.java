import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
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
      Path path = Path.of(currentPath);
      path = path.resolve("config_scheduler.yml");
      return readConfig(path.toString());
    } else {
      return readConfig("src/main/resources/config_scheduler.yml");
    }
  }

  public static Map<String, Object> readSshConfig() {
    if (executingJar) {
      Path path = Path.of(currentPath);
      path = path.resolve("config_ssh.yml");
      return readConfig(path.toString());
    } else {
      return readConfig("src/main/resources/config_ssh.yml");
    }
  }

  public static Map<String, Object> readInfluxConfig() {
    if (executingJar) {
      Path path = Path.of(currentPath);
      path = path.resolve("config_ssh.yml");
      return readConfig(path.toString());
    } else {
      return readConfig("src/main/resources/config_local.yml");
    }
  }
}
