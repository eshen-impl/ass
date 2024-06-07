import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(name = "HelloServlet", value = "/HelloServlet")
public class HelloServlet extends HttpServlet {
    private String msg;
    public void init() {
        // Initialization
        msg = "Hello World";
    }
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws  IOException {

        // Set response content type to text
        response.setContentType("text/html");

        // sleep for 1000ms. You can vary this value for different tests
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Send the response
        PrintWriter out = response.getWriter();
        out.println("<h1>" + msg + "</h1>");
    }

    public void destroy() {
        // nothing to do here
    }

//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//
//    }
}
