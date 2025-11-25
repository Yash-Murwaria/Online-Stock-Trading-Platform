/**
 * Single-file Java Web App (embedded Jetty) — Review-1 ready (no README)
 *
 * Features included (all in one file):
 * - Embedded Jetty server (servlets)
 * - H2 database (file-based) with schema execution and seeded users (BCrypt hashed)
 * - Connection pooling using HikariCP
 * - DAO + Service layers (as inner classes)
 * - Auth filter protecting endpoints
 * - Transaction example (update stock price)
 * - JSP-like simple view rendering via small template helpers (keeps single-file)
 * - Proper PreparedStatement usage, try-with-resources
 * - Password hashing using BCrypt
 *
 * Demo users created on first run:
 *   - student / student123
 *   - reviewer / reviewer123
 *
 * NOTE:
 *  - This single-file app is designed for demo/review. For production break into files,
 *    externalize configuration, and use real templates/JSPs or frameworks.
 *
 * Rubric image (for reference in your submission): /mnt/data/Screenshot 2025-11-25 at 9.13.33 PM.png
 *
 * Build (with Maven): include dependencies for Jetty, H2, HikariCP, jBCrypt (or add jars to classpath).
 * To run as a fat-jar, set mainClass to this file's class in your build tool.
 */

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mindrot.jbcrypt.BCrypt;
import org.h2.tools.RunScript;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import javax.sql.DataSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/* Top-level class */
public class JavaWebAppSingleFile {

    // JDBC config (H2 file DB in ./data)
    private static final String JDBC_URL = "jdbc:h2:./data/javawebdb;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASS = "";

    // DataSource (Hikari)
    private static HikariDataSource ds;

    public static void main(String[] args) throws Exception {
        // Create Jetty server
        Server server = new Server(8080);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/");

        // Provide JDBC attributes
        ctx.setAttribute("jdbc.url", JDBC_URL);
        ctx.setAttribute("jdbc.user", JDBC_USER);
        ctx.setAttribute("jdbc.pass", JDBC_PASS);

        // Initialize connection pool and put in context
        ds = createDataSource(JDBC_URL, JDBC_USER, JDBC_PASS);
        ctx.setAttribute("datasource", ds);

        // Initialize DB schema and seed (with hashed user passwords)
        initSchemaAndSeed(ctx);

        // Register servlets
        ctx.addServlet(new ServletHolder(new LoginServlet()), "/login");
        ctx.addServlet(new ServletHolder(new StocksServlet()), "/stocks");
        ctx.addServlet(new ServletHolder(new LogoutServlet()), "/logout");
        ctx.addServlet(new ServletHolder(new HomeServlet()), "/");

        // Register auth filter
        FilterHolder auth = new FilterHolder(new AuthFilter());
        ctx.addFilter(auth, "/*", EnumSet.of(DispatcherType.REQUEST));

        server.setHandler(ctx);
        try {
            server.start();
            System.out.println("Server started: http://localhost:8080/");
            server.join();
        } finally {
            if (ds != null) ds.close();
        }
    }

    /* -----------------------------
       DataSource (HikariCP)
       ----------------------------- */
    private static HikariDataSource createDataSource(String url, String user, String pass) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);
        config.setMaximumPoolSize(10);
        config.setPoolName("JavaWebAppPool");
        config.setAutoCommit(false);
        return new HikariDataSource(config);
    }

    /* -----------------------------
       Initialize Schema + Seed
       ----------------------------- */
    private static void initSchemaAndSeed(ServletContextHandler ctx) {
        String schemaSql = loadResourceAsString("schema_inline.sql");
        // schema inline if resource absent
        if (schemaSql == null) schemaSql = defaultSchemaSql();

        try (Connection c = ds.getConnection()) {
            // Use RunScript for multiple statements
            RunScript.execute(c, new StringReader(schemaSql));
            // Ensure default users exist with bcrypt hashed passwords
            UserDAO udao = new UserDAO(ds);
            if (udao.findByUsername("student").isEmpty()) {
                String hashed = BCrypt.hashpw("student123", BCrypt.gensalt(12));
                udao.insert(new User(0, "student", hashed, "Demo Student"));
            }
            if (udao.findByUsername("reviewer").isEmpty()) {
                String hashed = BCrypt.hashpw("reviewer123", BCrypt.gensalt(12));
                udao.insert(new User(0, "reviewer", hashed, "Project Reviewer"));
            }
            c.commit();
            System.out.println("Schema initialized & seeded.");
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private static String loadResourceAsString(String name) {
        try (InputStream is = JavaWebAppSingleFile.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static String defaultSchemaSql() {
        return String.join("\n",
            "DROP TABLE IF EXISTS users;",
            "CREATE TABLE users(",
            " id INT PRIMARY KEY AUTO_INCREMENT,",
            " username VARCHAR(50) UNIQUE NOT NULL,",
            " password VARCHAR(255) NOT NULL,",
            " fullname VARCHAR(100) NOT NULL",
            ");",
            "",
            "DROP TABLE IF EXISTS stocks;",
            "CREATE TABLE stocks(",
            " id INT PRIMARY KEY AUTO_INCREMENT,",
            " symbol VARCHAR(10) NOT NULL,",
            " name VARCHAR(150) NOT NULL,",
            " price DOUBLE NOT NULL",
            ");",
            "",
            "INSERT INTO stocks(symbol,name,price) VALUES",
            "('AAPL','Apple Inc.',175.12),",
            "('GOOG','Alphabet Inc.',134.45),",
            "('TSLA','Tesla Inc.',250.00);"
        );
    }

    /* =============================
       MODELS
       ============================= */
    public static class User {
        private int id;
        private String username;
        private String password; // hashed
        private String fullname;

        public User() {}
        public User(int id, String username, String password, String fullname) {
            this.id = id; this.username = username; this.password = password; this.fullname = fullname;
        }
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFullname() { return fullname; }
        public void setId(int id) { this.id = id; }
        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setFullname(String fullname) { this.fullname = fullname; }
    }

    public static class Stock {
        private int id;
        private String symbol;
        private String name;
        private double price;
        public Stock() {}
        public Stock(int id, String symbol, String name, double price) {
            this.id = id; this.symbol = symbol; this.name = name; this.price = price;
        }
        public int getId() { return id; }
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public void setId(int id) { this.id = id; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
        public void setName(String name) { this.name = name; }
        public void setPrice(double price) { this.price = price; }
    }

    /* =============================
       DAO LAYER
       ============================= */
    public static class UserDAO {
        private final DataSource ds;
        public UserDAO(DataSource ds) { this.ds = ds; }

        public Optional<User> findByUsername(String username) throws SQLException {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("fullname")
                        ));
                    }
                }
            }
            return Optional.empty();
        }

        public void insert(User u) throws SQLException {
            String sql = "INSERT INTO users(username,password,fullname) VALUES(?,?,?)";
            try (Connection c = ds.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getPassword());
                ps.setString(3, u.getFullname());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) u.setId(keys.getInt(1));
                }
                c.commit();
            }
        }
    }

    public static class StockDAO {
        private final DataSource ds;
        public StockDAO(DataSource ds) { this.ds = ds; }

        public List<Stock> findAll() throws SQLException {
            List<Stock> list = new ArrayList<>();
            String sql = "SELECT * FROM stocks ORDER BY symbol";
            try (Connection c = ds.getConnection();
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
            }
            return list;
        }

        // Transaction example: update price with commit/rollback
        public void updatePriceTransactional(int stockId, double newPrice) throws SQLException {
            String sql = "UPDATE stocks SET price = ? WHERE id = ?";
            Connection c = ds.getConnection();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                c.setAutoCommit(false);
                ps.setDouble(1, newPrice);
                ps.setInt(2, stockId);
                ps.executeUpdate();
                // COMMIT
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.close();
            }
        }
    }

    /* =============================
       SERVICE LAYER
       ============================= */
    public static class UserService {
        private final UserDAO dao;
        public UserService(DataSource ds) { this.dao = new UserDAO(ds); }

        public Optional<User> authenticate(String username, String plainPassword) throws SQLException {
            var opt = dao.findByUsername(username);
            if (opt.isPresent()) {
                User u = opt.get();
                if (BCrypt.checkpw(plainPassword, u.getPassword())) return Optional.of(u);
            }
            return Optional.empty();
        }

        public void register(String username, String plainPassword, String fullname) throws SQLException {
            String hashed = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
            dao.insert(new User(0, username, hashed, fullname));
        }
    }

    public static class StockService {
        private final StockDAO dao;
        public StockService(DataSource ds) { this.dao = new StockDAO(ds); }
        public List<Stock> getAllStocks() throws SQLException { return dao.findAll(); }
        public void updatePrice(int stockId, double price) throws SQLException { dao.updatePriceTransactional(stockId, price); }
    }

    /* =============================
       FILTER (Auth)
       ============================= */
    public static class AuthFilter implements Filter {
        @Override public void init(FilterConfig filterConfig) {}
        @Override public void destroy() {}

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;

            String path = req.getRequestURI();
            // Allow login and root
            if (path.equals("/") || path.endsWith("/login") || path.startsWith("/static/")) {
                chain.doFilter(request, response);
                return;
            }

            HttpSession s = req.getSession(false);
            if (s == null || s.getAttribute("user") == null) {
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            chain.doFilter(request, response);
        }
    }

    /* =============================
       SERVLETS
       ============================= */
    public static class HomeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            HttpSession s = req.getSession(false);
            if (s != null && s.getAttribute("user") != null) {
                resp.sendRedirect("stocks");
            } else {
                resp.sendRedirect("login");
            }
        }
    }

    public static class LoginServlet extends HttpServlet {
        private UserService userService;
        @Override
        public void init() {
            DataSource localDs = ds;
            this.userService = new UserService(localDs);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();
            out.println(htmlHeader("Login"));
            String err = (String) req.getAttribute("error");
            if (err != null) out.println("<p style='color:red'>" + escapeHtml(err) + "</p>");
            out.println("<form method='post' action='login'>");
            out.println("Username: <input name='username' required/><br/>");
            out.println("Password: <input type='password' name='password' required/><br/>");
            out.println("<button>Login</button>");
            out.println("</form>");
            out.println("<p>Demo: student / student123</p>");
            out.println(htmlFooter());
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
            String u = req.getParameter("username");
            String p = req.getParameter("password");
            try {
                Optional<User> user = userService.authenticate(u, p);
                if (user.isPresent()) {
                    HttpSession s = req.getSession(true);
                    s.setAttribute("user", user.get());
                    resp.sendRedirect("stocks");
                } else {
                    req.setAttribute("error", "Invalid credentials");
                    doGet(req, resp);
                }
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
        }
    }

    public static class StocksServlet extends HttpServlet {
        private StockService stockService;
        @Override
        public void init() {
            this.stockService = new StockService(ds);
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
            // require authenticated (AuthFilter already ensures)
            try {
                List<Stock> stocks = stockService.getAllStocks();
                resp.setContentType("text/html;charset=UTF-8");
                PrintWriter out = resp.getWriter();
                out.println(htmlHeader("Stocks"));
                out.println("<h2>Stocks</h2>");
                out.println("<table border='1' cellpadding='5'>");
                out.println("<tr><th>Symbol</th><th>Name</th><th>Price</th></tr>");
                for (Stock st : stocks) {
                    out.println("<tr><td>" + escapeHtml(st.getSymbol()) + "</td>");
                    out.println("<td>" + escapeHtml(st.getName()) + "</td>");
                    out.println("<td>" + st.getPrice() + "</td></tr>");
                }
                out.println("</table>");
                out.println("<p><a href='logout'>Logout</a></p>");
                out.println("<p>Admin action (demo transaction): <form method='post' action='stocks'>" +
                            "stockId: <input name='stockId' size='3'/> newPrice: <input name='price' size='6'/> <button>Update Price</button></form></p>");
                out.println(htmlFooter());
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
        }

        // Demonstrates transactional update (admin action for review)
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
            String sid = req.getParameter("stockId");
            String price = req.getParameter("price");
            if (sid == null || price == null || sid.isEmpty() || price.isEmpty()) {
                req.setAttribute("error", "stockId and price required");
                doGet(req, resp);
                return;
            }
            try {
                int stockId = Integer.parseInt(sid);
                double p = Double.parseDouble(price);
                stockService.updatePrice(stockId, p);
                resp.sendRedirect("stocks");
            } catch (NumberFormatException nfe) {
                req.setAttribute("error", "Invalid input format");
                doGet(req, resp);
            } catch (SQLException ex) {
                throw new ServletException(ex);
            }
        }
    }

    public static class LogoutServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            HttpSession s = req.getSession(false);
            if (s != null) s.invalidate();
            resp.sendRedirect("login");
        }
    }

    /* -------------------------
       Simple HTML helper & escaper
       ------------------------- */
    private static String htmlHeader(String title) {
        return "<!doctype html><html><head><meta charset='utf-8'><title>" + escapeHtml(title) + "</title></head><body>";
    }
    private static String htmlFooter() {
        return "</body></html>";
    }
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#x27;");
    }
}