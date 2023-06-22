package no.uio.scheduler;

import com.jcraft.jsch.*;
import java.io.*;
import java.util.List;
import java.util.Map;

public class SshSender {
  String host;
  String username;
  String password;
  int port;
  Map<String, Object> configMap;
  Session session = null;

  public SshSender(ConfigTypeEnum configType) {
    readConfig();
    setConfig(configType);
  }

  /**
   * Execute a list of commands on a remote server. The remote server depends on the actual
   * configuration set.
   *
   * @param cmdList list of commands to be executed
   */
  public void execCmds(List<String> cmdList) {

    try {
      ChannelExec channel;
      // set the configuration for the connection and connect
      session = new JSch().getSession(username, host, port);
      session.setPassword(password);
      // automatically accept and store the host key of the remote server without asking
      // confirmation
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      for (String cmd : cmdList) {
        // create a channel for executing commands on the server and set the command
        channel = (ChannelExec) session.openChannel("exec");

        channel.setCommand(cmd);

        // set the output stream from the channel in order to get the output of the executed command
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        channel.setOutputStream(responseStream);
        channel.connect();

        // wait for the command to be executed.
        // when the command is executed, the channel will be automatically disconnected by the
        // server.
        while (channel.isConnected()) {
          // Utils.printMessage("SSH command running... ", false);
          Thread.sleep(100);
        }

        printResult(cmd, responseStream.toString());

        // channel should be already disconnected, but just in case
        channel.disconnect();
      }

    } catch (JSchException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      if (session != null) {
        session.disconnect();
      }
    }
  }

  public void sendFile(String localPath, String remotePath) {
    try {
      // set the configuration for the connection and connect
      session = new JSch().getSession(username, host, port);
      session.setPassword(password);

      // automatically accept and store the host key of the remote server without asking
      // confirmation
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      // create a channel for sending files to the server
      ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
      sftpChannel.connect();

      // send the file
      sftpChannel.put(localPath, remotePath);

      sftpChannel.disconnect();

    } catch (JSchException e) {
      e.printStackTrace();
    } catch (SftpException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    } finally {
      if (session != null) {
        session.disconnect();
      }
    }
  }

  private void printResult(String cmd, String response) {
    Utils.printMessage("|*************| SSH: Executed command: " + cmd + " |*************|", false);
    System.out.println(response);
    Utils.printMessage("|********************************************************|", false);
  }

  public void readConfig() {
    configMap = Utils.readSshConfig();
  }

  public void setConfig(ConfigTypeEnum configType) {
    // TODO make this more generic
    if (configMap == null) {
      readConfig();
    }
    switch (configType) {
      case TEST -> {
        host = configMap.get("test_host").toString();
        username = configMap.get("test_username").toString();
        password = configMap.get("test_password").toString();
        port = Integer.parseInt(configMap.get("test_port").toString());
      }
      case ACTUATOR -> {
        host = configMap.get("actuator_host").toString();
        username = configMap.get("actuator_username").toString();
        password = configMap.get("actuator_password").toString();
        port = Integer.parseInt(configMap.get("actuator_port").toString());
      }
      case DATA_COLLECTOR_1 -> {
        host = configMap.get("shelf_1_data_collector_host").toString();
        username = configMap.get("shelf_1_data_collector_username").toString();
        password = configMap.get("shelf_1_data_collector_password").toString();
        port = Integer.parseInt(configMap.get("shelf_1_data_collector_port").toString());
      }
      case DATA_COLLECTOR_2 -> {
        host = configMap.get("shelf_2_data_collector_host").toString();
        username = configMap.get("shelf_2_data_collector_username").toString();
        password = configMap.get("shelf_2_data_collector_password").toString();
        port = Integer.parseInt(configMap.get("shelf_2_data_collector_port").toString());
      }
      default -> System.out.println("Config type not found");
    }
  }
}
