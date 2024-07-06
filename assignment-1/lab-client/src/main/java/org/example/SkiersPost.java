package org.example;

import io.swagger.client.*;
import io.swagger.client.api.SkiersApi;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SkiersPost {
    private static String basePath;
    private static final int TOTAL_REQUESTS = 200000;
    private static final int STARTUP_THREADS = 32;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int MAX_RETRIES = 5;
    private static final BlockingQueue<LiftRideEvent> requestQueue = new LinkedBlockingQueue<>();

    private static final AtomicInteger success = new AtomicInteger(0);
    private static final AtomicInteger failure = new AtomicInteger(0);

    public static void main(String[] args) {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            properties.load(fis);
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            return;
        }
        basePath = properties.getProperty("server.ip");
        if (basePath == null || basePath.isEmpty()) {
            System.err.println("Server IP not configured in config.properties");
            return;
        }

        long startTime = System.currentTimeMillis();
        new Thread(() -> {
            for (int i = 0; i < TOTAL_REQUESTS; i++) {
                try {
                    requestQueue.put(LiftRideEvent.generate());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        ExecutorService executorService = Executors.newFixedThreadPool(STARTUP_THREADS);
        for (int i = 0; i < STARTUP_THREADS; i++) {
            executorService.submit(() -> sendRequest(REQUESTS_PER_THREAD));
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(15, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int remainingPerThread =500;
        int remainingReqs = TOTAL_REQUESTS - (STARTUP_THREADS * REQUESTS_PER_THREAD);
        int additionalThreads = remainingReqs / remainingPerThread;
        executorService = Executors.newFixedThreadPool(additionalThreads);
        for (int i = 0; i < additionalThreads; i++) {
            executorService.submit(() -> sendRequest(remainingPerThread));
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(15, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        long endTime = System.currentTimeMillis();
        double throughput = (double) TOTAL_REQUESTS / ((endTime - startTime) / 1000.0);

        System.out.println("Number of successful requests sent: " + success.get());
        System.out.println("Number of unsuccessful requests: " + failure.get());
        System.out.println("Total run time: " + (endTime - startTime) + " ms");
        System.out.println("Total throughput: " + throughput + " requests per second");

    }

    private static void sendRequest(int reqCount) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(basePath);

        SkiersApi apiInstance = new SkiersApi(apiClient);

        for (int i = 0; i < reqCount; i++) {
            boolean isSuccess = false;
            int attempts = 0;
            while (!isSuccess && attempts < MAX_RETRIES) {
                try {
                    LiftRideEvent event = requestQueue.take();
                    apiInstance.writeNewLiftRide(
                            event.getLiftRide(),
                            event.getResortID(),
                            event.getSeasonID(),
                            event.getDayID(),
                            event.getSkierID()
                    );
                    success.incrementAndGet();
                    isSuccess = true;
                } catch (ApiException e) {
                    attempts++;
                    if (attempts >= MAX_RETRIES) {
                        failure.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}

