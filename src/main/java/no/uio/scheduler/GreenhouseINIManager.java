package no.uio.scheduler;

import java.util.List;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;

/**
 * Class providing methods for managing INI files. In particular managing INI configuration files
 * used by the Data Collectors.
 */
public class GreenhouseINIManager {
  public static void overwriteSection(
      INIConfiguration iniConfiguration, String section, String keyStart, List<String> values) {
    iniConfiguration.clearTree(section);

    int i = 1;
    for (String value : values) {
      String key = section + "." + keyStart + "_" + i++;
      iniConfiguration.addProperty(key, value);
    }
  }

  public static void printSection(SubnodeConfiguration section) {
    System.out.println("\t^^^^^^ INI SECTION: " + section.getRootElementName() + " ^^^^^^");
    section
        .getKeys()
        .forEachRemaining(
            key -> {
              System.out.println("\t\t" + key + " : " + section.getString(key));
            });
    System.out.println("\n");
  }

  public static void printFile(INIConfiguration iniConfiguration) {
    System.out.println("------------------ PRINTING INI FILE ------------------");
    iniConfiguration
        .getSections()
        .forEach(
            section -> {
              printSection(iniConfiguration.getSection(section));
            });
    System.out.println("\n\n");
  }
}
