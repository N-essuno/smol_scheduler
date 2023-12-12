package no.uio.scheduler;

import jakarta.jms.*;
import no.uio.microobject.runtime.REPL;
import org.apache.qpid.jms.JmsConnectionFactory;

import java.util.Arrays;

public class Subscriber {
    private final Connection connection;
    private final SmolScheduler scheduler;
    private final Utils utils;

    public Subscriber(String brokerURL, SmolScheduler scheduler, Utils utils) throws JMSException {
        this.scheduler = scheduler;
        this.utils = utils;
        ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerURL);
        connection = connectionFactory.createConnection();
        connection.start();
    }

    public void subscribe(String queueName) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queueName);

        MessageConsumer consumer = session.createConsumer(destination);

        consumer.setMessageListener(new MessageListener() {
            @Override
            public void onMessage(Message message) {
                utils.printMessage("Received message: " + message, false);
                try {
                    if (message instanceof TextMessage textMessage) {
                        String text = textMessage.getText();

                        String msg = text.split("[MSG]")[1];
                        // Process the received message as needed
                        utils.printMessage("Received message: " + msg, false);

                        if (queueName.equals("controller.1.asset.model")) {
                            REPL repl = scheduler.getRepl();
                            assert repl.getInterpreter() != null;
                            utils.printMessage("Reconfiguring AssetModel", false);
                            repl.getInterpreter().getTripleManager().regenerateTripleStoreModel();
                            repl.getInterpreter().evalCall(
                                    repl.getInterpreter().getObjectNames("AssetModel").get(0),
                                    "AssetModel",
                                    "reconfigure");
                        } else if (queueName.equals("controller.1.exec.time")) {
                            scheduler.setExecutionTime(Integer.parseInt(msg));
                        }
                    } else if (message instanceof BytesMessage) {
                        BytesMessage bytesMessage = (BytesMessage) message;
                        byte[] bytes = new byte[(int) bytesMessage.getBodyLength()];
                        bytesMessage.readBytes(bytes);
                        String msg = new String(bytes);
                        utils.printMessage("Received message: " + msg, false);

                        if (queueName.equals("controller.1.asset.model")) {
                            REPL repl = scheduler.getRepl();
                            assert repl.getInterpreter() != null;
                            utils.printMessage("Reconfiguring AssetModel", false);
                            repl.getInterpreter().getTripleManager().regenerateTripleStoreModel();
                            repl.getInterpreter().evalCall(
                                    repl.getInterpreter().getObjectNames("AssetModel").get(0),
                                    "AssetModel",
                                    "reconfigure");
                        } else if (queueName.equals("controller.1.exec.time")) {
                            scheduler.setExecutionTime(Integer.parseInt(msg));
                        }
                    }
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        });

        // Keep the program running to continue listening for messages
        // You can add a shutdown hook to gracefully close the JMS connection
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                session.close();
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }));
    }
}
