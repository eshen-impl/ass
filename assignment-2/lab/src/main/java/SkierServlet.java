
import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.util.Properties;
import com.rabbitmq.client.Channel;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import model.LiftRide;
import model.LiftRideEvent;
import rmqpool.RMQChannelFactory;
import rmqpool.RMQChannelPool;

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
    private static final String EXCHANGE_NAME = "LiftRide";
    private static final String QUEUE_NAME = "LiftRideQueue";
    private static final String ROUTING_KEY = "LiftRideKey";
    private RMQChannelPool channelPool;
    private Connection connection;
    private LiftRideEvent liftRideEvent;
    private Gson gson = new Gson();


    @Override
    public void init() throws ServletException {
        super.init();
        Properties properties = new Properties();
        try  {
            properties.load(getServletContext().getResourceAsStream("/WEB-INF/config.properties"));
        } catch (IOException e) {
            System.err.println("Error loading config.properties: " + e.getMessage());
            return;
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(properties.getProperty("rmq.host"));
            factory.setPort(Integer.parseInt(properties.getProperty("rmq.port")));
            factory.setUsername(properties.getProperty("rmq.username"));
            factory.setPassword(properties.getProperty("rmq.pwd"));
            this.connection = factory.newConnection();

            RMQChannelFactory channelFactory = new RMQChannelFactory(connection);
            this.channelPool = new RMQChannelPool(100, channelFactory);


        } catch (Exception e) {
            throw new ServletException("Failed to initialize RabbitMQ connection and channel pool", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }
        String[] urlParts = urlPath.split("/");
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("invalid url");
        } else {
            res.setStatus(HttpServletResponse.SC_OK);
            // TODO: process url params in `urlParts` for doGet
            int totalVertical = 34507;
            PrintWriter out = res.getWriter();
            out.print(totalVertical);
            out.flush();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("text/plain");
        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("missing parameters");
            return;
        }
        String[] urlParts = urlPath.split("/");
        if (!isUrlValid(urlParts)) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("invalid url");
            return;
        }

        StringBuilder reqBody = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                reqBody.append(line);
            }
            isReqBodyValid(reqBody.toString());
        } catch (IOException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("error reading request body");
            return;
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write(e.getMessage());
            return;
        }

        // TODO: write a new lift ride for the skier
        String message = gson.toJson(liftRideEvent);
        Channel channel = null;
        try {
            channel = channelPool.borrowObject();
            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes());
            res.setStatus(HttpServletResponse.SC_CREATED);
            res.getWriter().write("Success - New lift ride for skierID " + liftRideEvent.getSkierID());
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            if (channel != null) {
                try {
                    channelPool.returnObject(channel);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private void isReqBodyValid(String reqBody) {
        JsonObject jsonObject = gson.fromJson(reqBody.toString(), JsonObject.class);
        if (jsonObject.has("time") && jsonObject.has("liftID")) {
            int time = jsonObject.get("time").getAsInt();
            int liftID = jsonObject.get("liftID").getAsInt();
            if (time < 1 || time > 360 || liftID < 1 || liftID > 40) {
                throw new InvalidParameterException("invalid time or liftID");
            } else {
                liftRideEvent.setLiftRide(new LiftRide(time, liftID));
            }

        } else {
            throw new InvalidParameterException("missing time or liftID");
        }
    }


    private boolean isUrlValid(String[] urlPath) {
        // https://app.swaggerhub.com/apis/cloud-perf/SkiDataAPI/2.0#/skiers/writeNewLiftRide
        // /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
        // urlPath  = "/1/seasons/2019/days/1/skiers/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
//      // Data range rule refer to Assignment 1

        if (urlPath.length != 8) return false;
        liftRideEvent = new LiftRideEvent();
        // Check resortID
        try {
            int resortID = Integer.parseInt(urlPath[1]);
            if (resortID < 1 || resortID > 10) {
                return false;
            } else {
                liftRideEvent.setResortID(resortID);
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // Check seasons
        if (!urlPath[2].equals("seasons")) return false;

        liftRideEvent.setSeasonID(urlPath[3]);

        // Check days
        if (!urlPath[4].equals("days")) return false;

        // Check dayID
        try {
            int dayID = Integer.parseInt(urlPath[5]);
            if (dayID < 1 || dayID > 31) {
                return false;
            } else {
                liftRideEvent.setDayID(Integer.toString(dayID));
            }
        } catch (NumberFormatException e) {
            return false;
        }

        // Check skiers
        if (!urlPath[6].equals("skiers")) return false;

        // Check skierID
        try {
            int skierID = Integer.parseInt(urlPath[7]);
            if (skierID < 1 || skierID > 100000) {
                return false;
            } else {
                liftRideEvent.setSkierID(skierID);
            }
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
