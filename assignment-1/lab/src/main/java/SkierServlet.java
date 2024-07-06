
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws  IOException {
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
            // TODO: process url params in `urlParts`
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
        } else {
            StringBuilder reqBody = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    reqBody.append(line);
                }
            } catch (IOException e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write("error reading request body");
                return;
            }

            JsonObject jsonObject;
            try {
                jsonObject = new Gson().fromJson(reqBody.toString(), JsonObject.class);
                if (jsonObject.has("time") && jsonObject.has("liftID")) {
                    int time = jsonObject.get("time").getAsInt();
                    int liftID = jsonObject.get("liftID").getAsInt();
                    if (time <= 0 || liftID <= 0) {
                        throw new InvalidParameterException("invalid time or liftID");
                    }
                } else {
                    throw new InvalidParameterException("missing time or liftID");
                }
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getWriter().write(e.getMessage());
                return;
            }

            // TODO: write a new lift ride for the skier
            res.setStatus(HttpServletResponse.SC_CREATED);
            res.getWriter().write(reqBody.toString());
        }
    }


    private boolean isUrlValid(String[] urlPath) {
        // https://app.swaggerhub.com/apis/cloud-perf/SkiDataAPI/2.0#/skiers/writeNewLiftRide
        // /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
        // urlPath  = "/1/seasons/2019/days/1/skiers/123"
        // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]

        if (urlPath.length != 8) return false;

        // Check resortID
        try {
            Integer.parseInt(urlPath[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        // Check seasons
        if (!urlPath[2].equals("seasons")) return false;

        // Check seasonID
        try {
            Integer.parseInt(urlPath[3]);
        } catch (NumberFormatException e) {
            return false;
        }

        // Check days
        if (!urlPath[4].equals("days")) return false;

        // Check dayID
        try {
            Integer.parseInt(urlPath[5]);
        } catch (NumberFormatException e) {
            return false;
        }

        // Check skiers
        if (!urlPath[6].equals("skiers")) return false;

        // Check skierID
        try {
            Integer.parseInt(urlPath[7]);
        } catch (NumberFormatException e) {
            return false;
        }

        return true;
    }
}
