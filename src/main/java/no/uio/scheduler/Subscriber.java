package no.uio.scheduler;

import jakarta.jms.*;
import no.uio.microobject.runtime.REPL;
import org.apache.qpid.jms.JmsConnectionFactory;

public class Subscriber {
    private final Connection connection;
    private final SmolScheduler scheduler;

    public Subscriber(String brokerURL, SmolScheduler scheduler) throws JMSException {
        this.scheduler = scheduler;
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
                try {
                    if (message instanceof TextMessage) {
                        TextMessage textMessage = (TextMessage) message;
                        String text = textMessage.getText();

                        // Process the received message as needed
                        System.out.println("Received message: " + text);
                        REPL repl = scheduler.getRepl();
                        repl.getInterpreter().getTripleManager().regenerateTripleStoreModel();
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
}
