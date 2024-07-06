import com.rabbitmq.client.*;
import model.LiftRideEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;


public class Consumer {
    private static final String EXCHANGE_NAME = "LiftRide";
    private static final String QUEUE_NAME = "LiftRideQueue";
    private static final String ROUTING_KEY = "LiftRideKey";
    private static final int THREAD_POOL_SIZE = 100;
    private static final ConcurrentMap<Integer, LiftRideEvent> recordsMap = new ConcurrentHashMap<>();
    private static final Gson gson = new Gson();
    public static final AtomicInteger recordId = new AtomicInteger(0);
    public static void main(String[] args) throws InterruptedException {

        Properties properties = new Properties();
        try (InputStream fis = Consumer.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            return;
        }


        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.getProperty("rmq.host"));
        factory.setPort(Integer.parseInt(properties.getProperty("rmq.port")));
        factory.setUsername(properties.getProperty("rmq.username"));
        factory.setPassword(properties.getProperty("rmq.pwd"));

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);


        try {
            Connection connection = factory.newConnection();
            System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
            for (int i = 0; i < THREAD_POOL_SIZE; i++) {
                executor.submit(()-> {
                    try {
                        Channel channel = connection.createChannel();
                        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
                        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

                        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                            String message = new String(delivery.getBody(), "UTF-8");
                            LiftRideEvent event = gson.fromJson(message, LiftRideEvent.class);
                            int id = recordId.incrementAndGet();
                            recordsMap.put(id, event);
                        };

                        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
            Thread.currentThread().join();
        }



    }

}

