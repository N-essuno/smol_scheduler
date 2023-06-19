package no.uio.scheduler;

/**
 * Enum for the different types of configuration files. USed by the SsshSender to determine the IP
 * address and credentials to use for the connection.
 */
public enum ConfigTypeEnum {
  TEST,
  ACTUATOR,
  DATA_COLLECTOR_1,
  DATA_COLLECTOR_2
}
