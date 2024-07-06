package org.example;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;
public class SingleTest {
    private static final int TEST_REQUESTS = 10000;
    private static final String BASE_PATH = "http://ec2-35-87-112-68.us-west-2.compute.amazonaws.com:8080/lab_war/";

    public static void main(String[] args) {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(BASE_PATH);
        SkiersApi apiInstance = new SkiersApi(apiClient);


        long startTime = System.currentTimeMillis();

        for (int i = 0; i < TEST_REQUESTS; i++) {
            try {
                LiftRideEvent event = LiftRideEvent.generate();
                apiInstance.writeNewLiftRide(
                        event.getLiftRide(),
                        event.getResortID(),
                        event.getSeasonID(),
                        event.getDayID(),
                        event.getSkierID());
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double throughput = (double) TEST_REQUESTS / ((endTime - startTime) / 1000.0);

        System.out.println("Total time for " + TEST_REQUESTS + " requests: " + totalTime + " ms");
        System.out.println("Total throughput: " + throughput + " requests per second");
    }
}
