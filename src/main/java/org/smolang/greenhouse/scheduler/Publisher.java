package org.smolang.greenhouse.scheduler;

import org.apache.qpid.jms.JmsConnectionFactory;
import jakarta.jms.*;

public class Publisher {
    private final Connection connection;

    public Publisher(String brokerURL) throws JMSException {
        ConnectionFactory connectionFactory = new JmsConnectionFactory(brokerURL);
        connection = connectionFactory.createConnection();
        connection.start();
    }

    public void publish(String queueName, String message) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(queueName);

        MessageProducer producer = session.createProducer(destination);
        TextMessage textMessage = session.createTextMessage(message);

        producer.send(textMessage);
        session.close();
        connection.close();
    }
}
