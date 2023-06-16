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

  public void execCmds(List<String> cmdList) {
    try {
      ChannelExec channel;
      // Set the configuration for the connection and connect
      session = new JSch().getSession(username, host, port);
      session.setPassword(password);
      // Automatically accept and store the host key of the remote server without asking
      // confirmation
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      for (String cmd : cmdList) {
        // Create a channel for executing commands on the server and set the command
        channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(cmd);

        // Set the output stream from the channel in order to get the output of the executed command
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        channel.setOutputStream(responseStream);
        channel.connect();

        // Wait for the command to be executed.
        // When the command is executed, the channel will be automatically disconnected by the
        // server.
        while (channel.isConnected()) {
          Thread.sleep(100);
        }

        printResult(cmd, responseStream.toString());

        // Channel should be already disconnected, but just in case

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
      // Set the configuration for the connection and connect
      session = new JSch().getSession(username, host, port);
      session.setPassword(password);

      // Automatically accept and store the host key of the remote server without asking
      // confirmation
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      // Create a channel for sending files to the server
      ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
      sftpChannel.connect();

      // Send the file
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
    System.out.println("|*************| SSH: Executed command: " + cmd + " |*************|");
    System.out.println(response);
    System.out.println("|********************************************************|");
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
      case TEST:
        host = configMap.get("test_host").toString();
        username = configMap.get("test_username").toString();
        password = configMap.get("test_password").toString();
        port = Integer.parseInt(configMap.get("test_port").toString());
        break;
      case ACTUATOR:
        host = configMap.get("actuator_host").toString();
        username = configMap.get("actuator_username").toString();
        password = configMap.get("actuator_password").toString();
        port = Integer.parseInt(configMap.get("actuator_port").toString());
        break;
      case DATA_COLLECTOR_1:
        host = configMap.get("shelf_1_data_collector_host").toString();
        username = configMap.get("shelf_1_data_collector_username").toString();
        password = configMap.get("shelf_1_data_collector_password").toString();
        port = Integer.parseInt(configMap.get("shelf_1_data_collector_port").toString());
        break;
      default:
        System.out.println("Config type not found");
    }
  }
}
