import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Utils {

    public static boolean executingJar = true;
    private static final String currentPath = Utils.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .getPath().replace("smol_scheduler.jar", "");

    public static Map<String, Object> readConfig(String configPath){
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

    public static Map<String, Object> readSchedulerConfig(){
        if (executingJar) {
            return readConfig(currentPath+"\\config_scheduler.yml");
        } else {
            return readConfig("src/main/resources/config_scheduler.yml");
        }
    }

    public static Map<String, Object> readSshConfig(){
        if (executingJar) {
            return readConfig(currentPath+"\\config_ssh.yml");
        } else {
            return readConfig("src/main/resources/config_ssh.yml");
        }
    }

    public static Map<String, Object> readInfluxConfig(){
        if (executingJar) {
            return readConfig(currentPath+"\\config_local.yml");
        } else {
            return readConfig("src/main/resources/config_local.yml");
        }
    }

}
