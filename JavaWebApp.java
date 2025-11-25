/*
File: JavaWebApp.java
Single-file Java web app (embedded Jetty + H2) implementing:
- Problem understanding & solution design
- Core Java concepts: POJOs, interfaces, service layer, separation of concerns
- Database integration (JDBC) with H2 (schema + seed)
- Servlets & Web integration (LoginServlet, StocksServlet)

Author: Generated for Review-1 demo
Notes:
 - This is a demo app. Passwords are plaintext for simplicity (DO NOT do this in production).
 - Uses Jetty as embedded servlet container (Maven dependencies required).
 - Stores H2 DB file in the user home (~/.javawebdb by default) for persistence across runs.
*/

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.h2.tools.RunScript;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletConfig;

import java.io.*;
import java.sql.*;
import java.util.*;

/* --------------------------
   PROBLEM UNDERSTANDING (short)
   --------------------------
   Build a small online stock listing web demo where:
   - Users can login with credentials stored in DB
   - After login, users can view available stocks (symbol, name, price)
   - Demonstrate DB integration (JDBC), core Java design (DAO, Service, Model),
     and Servlets for web integration.
*/

public class JavaWebApp {

    // JDBC configuration
    public static final String JDBC_URL = "jdbc:h2:~/javawebdb";
    public static final String JDBC_USER = "sa";
    public static final String JDBC_PASS = "";

    /* ------------------
       Main: start server
       ------------------ */
    public static void main(String[] args) throws Exception {

        initDatabase();

        Server server = new Server(8080);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/");
        server.setHandler(ctx);

        // Set DB config in servlet context
        ctx.setAttribute("jdbc.url", JDBC_URL);
        ctx.setAttribute("jdbc.user", JDBC_USER);
        ctx.setAttribute("jdbc.pass", JDBC_PASS);

        // Map servlets
        ctx.addServlet(LoginServlet.class, "/login");
        ctx.addServlet(StocksServlet.class, "/stocks");
        ctx.addServlet(LogoutServlet.class, "/logout");
        ctx.addServlet(HomeServlet.class, "/");

        System.out.println("Server running → http://localhost:8080/");
        server.start();
        server.join();
    }

    /* ------------------------
       Initialize H2 DB
       ------------------------ */
    private static void initDatabase() {
        String schema = getSchemaSql();
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASS)) {
            RunScript.execute(conn, new StringReader(schema));
            System.out.println("Database initialized.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getSchemaSql() {
        return String.join("\n",
            "DROP TABLE IF EXISTS users;",
            "CREATE TABLE users(",
            " id INT PRIMARY KEY AUTO_INCREMENT,",
            " username VARCHAR(50) UNIQUE NOT NULL,",
            " password VARCHAR(100) NOT NULL,",
            " fullname VARCHAR(100)",
            ");",
            "",
            "DROP TABLE IF EXISTS stocks;",
            "CREATE TABLE stocks(",
            " id INT PRIMARY KEY AUTO_INCREMENT,",
            " symbol VARCHAR(10),",
            " name VARCHAR(100),",
            " price DOUBLE",
            ");",
            "",
            "INSERT INTO users(username, password, fullname) VALUES",
            "('student','student123','Demo Student'),",
            "('reviewer','reviewer123','Project Reviewer');",
            "",
            "INSERT INTO stocks(symbol,name,price) VALUES",
            "('AAPL','Apple Inc.',175.12),",
            "('GOOG','Alphabet Inc.',134.45),",
            "('TSLA','Tesla Inc.',250.00);"
        );
    }

    /* -----------------------------------
       Models: User & Stock (POJOs)
       ----------------------------------- */

    public static class User {
        private int id;
        private String username;
        private String password;
        private String fullname;

        public User() {}

        public User(int id, String username, String password, String fullname) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.fullname = fullname;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFullname() { return fullname; }
    }

    public static class Stock {
        private int id;
        private String symbol;
        private String name;
        private double price;

        public Stock(int id, String symbol, String name, double price) {
            this.id = id;
            this.symbol = symbol;
            this.name = name;
            this.price = price;
        }

        public int getId() { return id; }
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getPrice() { return price; }
    }

    /* -----------------
       DAO Layer
       ----------------- */

    public static class DBUtil {
        public static Connection getConnection(ServletContext ctx) throws SQLException {
            return DriverManager.getConnection(
                (String) ctx.getAttribute("jdbc.url"),
                (String) ctx.getAttribute("jdbc.user"),
                (String) ctx.getAttribute("jdbc.pass")
            );
        }
    }

    public static class UserDAO {
        private final ServletContext ctx;
        public UserDAO(ServletContext ctx) { this.ctx = ctx; }

        public Optional<User> findByUsername(String username) {
            String sql = "SELECT * FROM users WHERE username=?";
            try (Connection c = DBUtil.getConnection(ctx);
                 PreparedStatement ps = c.prepareStatement(sql)) {

                ps.setString(1, username);

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return Optional.of(new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("fullname")
                    ));
                }
            } catch (Exception e) { e.printStackTrace(); }
            return Optional.empty();
        }
    }

    public static class StockDAO {
        private final ServletContext ctx;
        public StockDAO(ServletContext ctx) { this.ctx = ctx; }

        public List<Stock> findAll() {
            List<Stock> list = new ArrayList<>();
            String sql = "SELECT * FROM stocks ORDER BY symbol";

            try (Connection c = DBUtil.getConnection(ctx);
                 PreparedStatement ps = c.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    list.add(new Stock(
                        rs.getInt("id"),
                        rs.getString("symbol"),
                        rs.getString("name"),
                        rs.getDouble("price")
                    ));
                }
            } catch (Exception e) { e.printStackTrace(); }
            return list;
        }
    }

    /* -----------------
       Service Layer
       ----------------- */

    public interface StockService {
        List<Stock> getAllStocks();
    }

    public static class StockServiceImpl implements StockService {
        private final StockDAO dao;
        public StockServiceImpl(ServletContext ctx) {
            this.dao = new StockDAO(ctx);
        }
        public List<Stock> getAllStocks() {
            return dao.findAll();
        }
    }

    /* -----------------
       Servlets
       ----------------- */

    @WebServlet("/")
    public static class HomeServlet extends HttpServlet {
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            HttpSession s = req.getSession(false);
            if (s != null && s.getAttribute("user") != null)
                resp.sendRedirect("stocks");
            else
                resp.sendRedirect("login");
        }
    }

    @WebServlet("/login")
    public static class LoginServlet extends HttpServlet {
        private UserDAO dao;

        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            dao = new UserDAO(config.getServletContext());
        }

        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html");

            PrintWriter out = resp.getWriter();
            out.println(htmlHeader("Login"));
            String err = (String) req.getAttribute("error");
            if (err != null) out.println("<p style='color:red'>" + err + "</p>");

            out.println("<form method='post'>");
            out.println("Username: <input name='username'/><br/>");
            out.println("Password: <input type='password' name='password'/><br/>");
            out.println("<button>Login</button>");
            out.println("</form>");

            out.println("<p>Demo: student/student123</p>");
            out.println(htmlFooter());
        }

        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
            String u = req.getParameter("username");
            String p = req.getParameter("password");

            Optional<User> user = dao.findByUsername(u);

            if (user.isPresent() && user.get().getPassword().equals(p)) {
                HttpSession s = req.getSession(true);
                s.setAttribute("user", user.get());
                resp.sendRedirect("stocks");
            } else {
                req.setAttribute("error", "Invalid credentials");
                doGet(req, resp);
            }
        }
    }

    @WebServlet("/stocks")
    public static class StocksServlet extends HttpServlet {
        private StockService service;

        public void init(ServletConfig config) throws ServletException {
            super.init(config);
            service = new StockServiceImpl(config.getServletContext());
        }

        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            HttpSession s = req.getSession(false);
            if (s == null || s.getAttribute("user") == null) {
                resp.sendRedirect("login");
                return;
            }

            List<Stock> list = service.getAllStocks();

            resp.setContentType("text/html");
            PrintWriter out = resp.getWriter();

            out.println(htmlHeader("Stocks"));
            out.println("<h2>Stocks</h2>");

            out.println("<table border='1' cellpadding='5'>");
            out.println("<tr><th>Symbol</th><th>Name</th><th>Price</th></tr>");
            for (Stock st : list) {
                out.println("<tr>");
                out.println("<td>" + st.getSymbol() + "</td>");
                out.println("<td>" + st.getName() + "</td>");
                out.println("<td>" + st.getPrice() + "</td>");
                out.println("</tr>");
            }
            out.println("</table>");

            out.println("<p><a href='logout'>Logout</a></p>");
            out.println(htmlFooter());
        }
    }

    @WebServlet("/logout")
    public static class LogoutServlet extends HttpServlet {
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            HttpSession s = req.getSession(false);
            if (s != null) s.invalidate();
            resp.sendRedirect("login");
        }
    }

    /* --------------
       HTML helpers
       -------------- */
    private static String htmlHeader(String title) {
        return "<html><head><title>" + title + "</title></head><body>";
    }

    private static String htmlFooter() {
        return "</body></html>";
    }
}