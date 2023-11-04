package no.uio.scheduler;

import java.util.List;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;

/**
 * Class providing methods for managing INI files. In particular managing INI configuration files
 * used by the Data Collectors.
 */
public class GreenhouseINIManager {
  private Utils utils;

  public GreenhouseINIManager(Utils utils) {
    this.utils = utils;
  }

  /**
   * Overwrite properties in a given section. The properties will be formatted as follows:
   * <section>.<keyStart>_<i> = <value> where i is an integer starting from 1 and increasing by 1
   * for each value in the list of values
   */
  public void overwriteSection(
      INIConfiguration iniConfiguration, String section, String keyStart, List<String> values) {

    // delete all properties in section
    iniConfiguration.clearTree(section);

    int i = 1;
    for (String value : values) {
      String key = section + "." + keyStart + "_" + i++;
      iniConfiguration.addProperty(key, value);
    }
  }

  public void overwriteSection(
      INIConfiguration iniConfiguration, String section, String keyStart, String value) {

    // delete all properties in section
    iniConfiguration.clearTree(section);

    String key = section + "." + keyStart + "_" + "1";
    iniConfiguration.addProperty(key, value);
  }

  public void printSection(SubnodeConfiguration section) {
    this.utils.printMessage("\t INI SECTION: " + section.getRootElementName(), false);
    section
        .getKeys()
        .forEachRemaining(
            key -> this.utils.printMessage("\t\t" + key + " : " + section.getString(key), false));
    System.out.println("\n");
  }

  public void printFile(INIConfiguration iniConfiguration) {
    this.utils.printMessage("Printing INI file:", false);
    iniConfiguration
        .getSections()
        .forEach(
            section -> {
              printSection(iniConfiguration.getSection(section));
            });
    System.out.println("\n\n");
  }
}
