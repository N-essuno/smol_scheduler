package no.uio.scheduler;

import java.util.List;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;

/**
 * Class providing methods for managing INI files. In particular managing INI configuration files
 * used by the Data Collectors.
 */
public class GreenhouseINIManager {

  /**
   * Overwrite properties in a given section. The properties will be formatted as follows:
   * <section>.<keyStart>_<i> = <value> where i is an integer starting from 1 and increasing by 1
   * for each value in the list of values
   */
  public static void overwriteSection(
      INIConfiguration iniConfiguration, String section, String keyStart, List<String> values) {

    // delete all properties in section
    iniConfiguration.clearTree(section);

    int i = 1;
    for (String value : values) {
      String key = section + "." + keyStart + "_" + i++;
      iniConfiguration.addProperty(key, value);
    }
  }

  public static void printSection(SubnodeConfiguration section) {
    Utils.printMessage("\t INI SECTION: " + section.getRootElementName(), false);
    section
        .getKeys()
        .forEachRemaining(
            key -> Utils.printMessage("\t\t" + key + " : " + section.getString(key), false));
    System.out.println("\n");
  }

  public static void printFile(INIConfiguration iniConfiguration) {
    Utils.printMessage("Printing INI file:", false);
    iniConfiguration
        .getSections()
        .forEach(
            section -> {
              printSection(iniConfiguration.getSection(section));
            });
    System.out.println("\n\n");
  }
}
