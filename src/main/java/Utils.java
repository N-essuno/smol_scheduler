import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class Utils {

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
}
