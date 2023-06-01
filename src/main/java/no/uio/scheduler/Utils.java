package no.uio.scheduler;

import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class Utils {

  public static Map<String, Object> readSchedulerConfig() {
    return new Yaml().load(Utils.class.getResourceAsStream("config_scheduler.yml"));
  }

  public static Map<String, Object> readSshConfig() {
    return new Yaml().load(Utils.class.getResourceAsStream("config_ssh.yml"));
  }

  public static Map<String, Object> readInfluxConfig() {
    return new Yaml().load(Utils.class.getResourceAsStream("config_local.yml"));
  }
}
