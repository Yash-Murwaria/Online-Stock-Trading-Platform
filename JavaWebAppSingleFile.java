/**
 * Single-file Java Web App (embedded Jetty) — Review-2 READY
 *
 * Review-1 Features:
 * - Embedded Jetty server (servlets)
 * - H2 database (file-based) with schema execution and seed
 * - HikariCP connection pooling
 * - DAO + Service layers
 * - Auth filter
 * - Transaction example (stock update)
 * - PreparedStatements, try-with-resources
 * - BCrypt password hashing
 *
 * Review-2 Enhancements:
 * - Test-friendly UserService (constructor injection)
 * - Authentication logic isolated for unit testing
 * - Explicit testing intent markers
 *
 * Demo users:
 *  - student / student123
 *  - reviewer / reviewer123
 */

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;

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

public class JavaWebAppSingleFile {

    /* =============================
       CONFIG
       ============================= */
    private static final String JDBC_URL  = "jdbc:h2:./data/javawebdb;AUTO_SERVER=TRUE";
    private static final String JDBC_USER = "sa";
    private static final String JDBC_PASS = "";

    private static HikariDataSource ds;

    /* =============================
       MAIN
       ============================= */
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        ServletContextHandler ctx =
                new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/");

        ds = createDataSource(JDBC_URL, JDBC_USER, JDBC_PASS);
        ctx.setAttribute("datasource", ds);

        initSchemaAndSeed();

        ctx.addServlet(new ServletHolder(new HomeServlet()), "/");
        ctx.addServlet(new ServletHolder(new LoginServlet()), "/login");
        ctx.addServlet(new ServletHolder(new StocksServlet()), "/stocks");
        ctx.addServlet(new ServletHolder(new LogoutServlet()), "/logout");

        ctx.addFilter(new FilterHolder(new AuthFilter()),
                "/*", EnumSet.of(DispatcherType.REQUEST));

        server.setHandler(ctx);
        server.start();
        System.out.println("Server running at http://localhost:8080/");
        server.join();
    }

    /* =============================
       DATASOURCE
       ============================= */
    private static HikariDataSource createDataSource(String url, String user, String pass) {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(10);
        cfg.setAutoCommit(false);
        return new HikariDataSource(cfg);
    }

    // REVIEW-2: exposed only for unit tests
    static DataSource getDataSourceForTests() {
        return ds;
    }

    /* =============================
       DB INIT
       ============================= */
    private static void initSchemaAndSeed() {
        try (Connection c = ds.getConnection()) {
            RunScript.execute(c, new StringReader(defaultSchemaSql()));

            UserDAO dao = new UserDAO(ds);
            if (dao.findByUsername("student").isEmpty()) {
                dao.insert(new User(0, "student",
                        BCrypt.hashpw("student123", BCrypt.gensalt(12)),
                        "Demo Student"));
            }
            if (dao.findByUsername("reviewer").isEmpty()) {
                dao.insert(new User(0, "reviewer",
                        BCrypt.hashpw("reviewer123", BCrypt.gensalt(12)),
                        "Project Reviewer"));
            }
            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private static String defaultSchemaSql() {
        return String.join("\n",
            "DROP TABLE IF EXISTS users;",
            "CREATE TABLE users(",
            " id INT AUTO_INCREMENT PRIMARY KEY,",
            " username VARCHAR(50) UNIQUE,",
            " password VARCHAR(255),",
            " fullname VARCHAR(100)",
            ");",
            "",
            "DROP TABLE IF EXISTS stocks;",
            "CREATE TABLE stocks(",
            " id INT AUTO_INCREMENT PRIMARY KEY,",
            " symbol VARCHAR(10),",
            " name VARCHAR(150),",
            " price DOUBLE",
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
        private String password;
        private String fullname;

        public User(int id, String u, String p, String f) {
            this.id = id; this.username = u; this.password = p; this.fullname = f;
        }
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getFullname() { return fullname; }
    }

    public static class Stock {
        int id; String symbol; String name; double price;
        public Stock(int i, String s, String n, double p) {
            id=i; symbol=s; name=n; price=p;
        }
    }

    /* =============================
       DAO LAYER
       ============================= */
    public static class UserDAO {
        private final DataSource ds;
        public UserDAO(DataSource ds) { this.ds = ds; }

        public Optional<User> findByUsername(String u) throws SQLException {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps =
                     c.prepareStatement("SELECT * FROM users WHERE username=?")) {
                ps.setString(1, u);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(new User(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getString("fullname")));
                    }
                }
            }
            return Optional.empty();
        }

        public void insert(User u) throws SQLException {
            try (Connection c = ds.getConnection();
                 PreparedStatement ps =
                     c.prepareStatement(
                        "INSERT INTO users(username,password,fullname) VALUES(?,?,?)")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, u.getPassword());
                ps.setString(3, u.getFullname());
                ps.executeUpdate();
                c.commit();
            }
        }
    }

    public static class StockDAO {
        private final DataSource ds;
        public StockDAO(DataSource ds) { this.ds = ds; }

        public List<Stock> findAll() throws SQLException {
            List<Stock> list = new ArrayList<>();
            try (Connection c = ds.getConnection();
                 PreparedStatement ps =
                     c.prepareStatement("SELECT * FROM stocks ORDER BY symbol");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Stock(
                            rs.getInt("id"),
                            rs.getString("symbol"),
                            rs.getString("name"),
                            rs.getDouble("price")));
                }
            }
            return list;
        }

        // Transaction example
        public void updatePriceTransactional(int id, double price) throws SQLException {
            Connection c = ds.getConnection();
            try (PreparedStatement ps =
                     c.prepareStatement("UPDATE stocks SET price=? WHERE id=?")) {
                c.setAutoCommit(false);
                ps.setDouble(1, price);
                ps.setInt(2, id);
                ps.executeUpdate();
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
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

        // production
        public UserService(DataSource ds) {
            this.dao = new UserDAO(ds);
        }

        // REVIEW-2: constructor injection for unit tests
        UserService(UserDAO dao) {
            this.dao = dao;
        }

        public Optional<User> authenticate(String u, String p) throws SQLException {
            Optional<User> opt = dao.findByUsername(u);
            if (opt.isPresent() && passwordMatches(p, opt.get().getPassword())) {
                return opt;
            }
            return Optional.empty();
        }

        // REVIEW-2: isolated logic for testing
        boolean passwordMatches(String plain, String hashed) {
            return BCrypt.checkpw(plain, hashed);
        }
    }

    public static class StockService {
        private final StockDAO dao;
        public StockService(DataSource ds) { dao = new StockDAO(ds); }
        public List<Stock> getAllStocks() throws SQLException { return dao.findAll(); }
        public void updatePrice(int id, double p) throws SQLException {
            dao.updatePriceTransactional(id, p);
        }
    }

    /* =============================
       AUTH FILTER
       ============================= */
    public static class AuthFilter implements Filter {
        public void doFilter(ServletRequest r, ServletResponse s, FilterChain c)
                throws IOException, ServletException {
            HttpServletRequest req = (HttpServletRequest) r;
            HttpSession session = req.getSession(false);
            if (req.getRequestURI().endsWith("/login") ||
                req.getRequestURI().equals("/") ||
                session != null) {
                c.doFilter(r, s);
            } else {
                ((HttpServletResponse) s).sendRedirect("login");
            }
        }
    }

    /* =============================
       SERVLETS
       ============================= */
    public static class HomeServlet extends HttpServlet {
        protected void doGet(HttpServletRequest r, HttpServletResponse s)
                throws IOException {
            HttpSession sess = r.getSession(false);
            s.sendRedirect(sess == null ? "login" : "stocks");
        }
    }

    public static class LoginServlet extends HttpServlet {
        private UserService service;
        public void init() { service = new UserService(ds); }

        protected void doGet(HttpServletRequest r, HttpServletResponse s)
                throws IOException {
            s.setContentType("text/html");
            s.getWriter().println("""
                <form method='post'>
                  Username: <input name='username'/><br/>
                  Password: <input type='password' name='password'/><br/>
                  <button>Login</button>
                  <p>Demo: student / student123</p>
                </form>
            """);
        }

        protected void doPost(HttpServletRequest r, HttpServletResponse s)
                throws IOException {
            try {
                Optional<User> u =
                        service.authenticate(
                                r.getParameter("username"),
                                r.getParameter("password"));
                if (u.isPresent()) {
                    r.getSession(true).setAttribute("user", u.get());
                    s.sendRedirect("stocks");
                } else {
                    s.getWriter().println("Invalid credentials");
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class StocksServlet extends HttpServlet {
        private StockService service;
        public void init() { service = new StockService(ds); }

        protected void doGet(HttpServletRequest r, HttpServletResponse s)
                throws IOException {
            try {
                s.setContentType("text/html");
                for (Stock st : service.getAllStocks()) {
                    s.getWriter().println(
                            st.symbol + " : " + st.price + "<br/>");
                }
                s.getWriter().println("<a href='logout'>Logout</a>");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class LogoutServlet extends HttpServlet {
        protected void doGet(HttpServletRequest r, HttpServletResponse s)
                throws IOException {
            r.getSession().invalidate();
            s.sendRedirect("login");
        }
    }
}
