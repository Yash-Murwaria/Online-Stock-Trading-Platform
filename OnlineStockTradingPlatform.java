/**
 * OnlineStockTradingPlatform.java
 *
 * Single-file Java program demonstrating:
 *  - MVC structure (flattened into nested classes for single-file delivery)
 *  - OOP: inheritance, polymorphism, interfaces, custom exceptions
 *  - Collections & Generics
 *  - Multithreading & Synchronization
 *  - DAO classes with JDBC code (with automatic fallback to in-memory storage if JDBC unavailable)
 *  - Basic Swing GUI for Login, AdminDashboard, TraderDashboard
 *
 * Notes:
 *  - To use a real DB (MySQL for example), set DBConnection.URL/USER/PASS and ensure driver is on classpath.
 *  - If JDBC fails, the code uses an in-memory Map + CSV files in working dir to simulate persistence.
 *
 * This file intentionally contains comprehensive inline comments indicating which rubric item
 * is implemented where. Use it as a reference and as a starting point for your project.
 */

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class OnlineStockTradingPlatform {

    /*************************************************************************
     * Custom Exceptions (Step 2: Exception Handling)
     *************************************************************************/
    public static class InvalidTradeException extends Exception {
        public InvalidTradeException(String msg) { super(msg); }
    }

    public static class DatabaseException extends Exception {
        public DatabaseException(String msg, Throwable cause) { super(msg, cause); }
    }

    /*************************************************************************
     * Interfaces (Step 2: Interfaces)
     *************************************************************************/
    public interface TradeOperations {
        void buyStock(int traderId, String symbol, int quantity) throws InvalidTradeException, DatabaseException;
        void sellStock(int traderId, String symbol, int quantity) throws InvalidTradeException, DatabaseException;
    }

    /*************************************************************************
     * Models (Step 1: Design models)
     * - User (abstract) -> Admin, Trader (Inheritance + Polymorphism)
     *************************************************************************/
    public static abstract class User {
        protected int id;
        protected String name;
        protected String email;
        public User(int id, String name, String email) { this.id = id; this.name = name; this.email = email; }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public abstract void displayDashboard(); // polymorphic method
    }

    public static class Admin extends User {
        public Admin(int id, String name, String email) { super(id, name, email); }
        @Override
        public void displayDashboard() {
            System.out.println("Admin dashboard for " + name);
        }
    }

    public static class Trader extends User {
        public Trader(int id, String name, String email) { super(id, name, email); }
        @Override
        public void displayDashboard() {
            System.out.println("Trader dashboard for " + name);
        }
    }

    public static class Stock {
        private String symbol;
        private String name;
        private double price; // latest price

        public Stock(String symbol, String name, double price) {
            this.symbol = symbol; this.name = name; this.price = price;
        }
        public String getSymbol() { return symbol; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public void setPrice(double p) { this.price = p; }
    }

    public static class Trade {
        private int id;
        private int traderId;
        private String symbol;
        private int quantity;
        private double price;
        private LocalDateTime timestamp;
        private String type; // "BUY" or "SELL"

        public Trade(int id, int traderId, String symbol, int quantity, double price, String type) {
            this.id = id; this.traderId = traderId; this.symbol = symbol; this.quantity = quantity;
            this.price = price; this.timestamp = LocalDateTime.now(); this.type = type;
        }
        public int getTraderId() { return traderId; }
        public String getSymbol() { return symbol; }
        public int getQuantity() { return quantity; }
        public double getPrice() { return price; }
        public String getType() { return type; }
        public String toCSV() {
            return String.format("%d,%d,%s,%d,%.2f,%s,%s", id, traderId, symbol, quantity, price, type, timestamp.toString());
        }
    }

    public static class Portfolio {
        // Use Generics and Collections - Step 3
        // Map<symbol, quantity>
        private Map<String, Integer> holdings = new TreeMap<>();
        public Map<String,Integer> getHoldings() { return holdings; }

        public synchronized void add(String symbol, int qty) {
            holdings.put(symbol, holdings.getOrDefault(symbol, 0) + qty);
        }
        public synchronized void remove(String symbol, int qty) {
            holdings.put(symbol, Math.max(0, holdings.getOrDefault(symbol,0) - qty));
            if (holdings.get(symbol) == 0) holdings.remove(symbol);
        }
        public int getQuantity(String symbol) {
            return holdings.getOrDefault(symbol, 0);
        }
    }

    /*************************************************************************
     * DB Connection (Step 6: JDBC Connectivity)
     *
     * This class tries to create a JDBC connection using provided constants.
     * If connection is unavailable, getConnection() returns null and DAOs
     * automatically switch to fallback persistence (in-memory + CSV).
     *************************************************************************/
    public static class DBConnection {
        // Edit if you want real DB; for MySQL example:
        // public static final String URL = "jdbc:mysql://localhost:3306/tradingdb?useSSL=false&serverTimezone=UTC";
        // public static final String USER = "root";
        // public static final String PASS = "password";

        // Default: invalid URL so the program falls back to file-based persistence.
        public static final String URL = "jdbc:mysql://localhost:3306/tradingdb?useSSL=false&serverTimezone=UTC";
        public static final String USER = "root";
        public static final String PASS = "password";

        private static Connection conn = null;
        private static boolean attempted = false;

        /**
         * Tries to return a JDBC connection. If the connection cannot be established
         * (driver not found or DB not reachable), this returns null and code should
         * use fallback persistence.
         */
        public static Connection getConnection() {
            if (attempted) return conn;
            attempted = true;
            try {
                conn = DriverManager.getConnection(URL, USER, PASS);
                System.out.println("[DB] JDBC connection established.");
            } catch (SQLException ex) {
                System.err.println("[DB] JDBC connection failed: " + ex.getMessage());
                conn = null;
            }
            return conn;
        }

        /**
         * Optionally create required tables if JDBC is available.
         */
        public static void ensureTablesExist() {
            Connection c = getConnection();
            if (c == null) return;
            try (Statement st = c.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS users (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(100), role VARCHAR(10))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS stocks (symbol VARCHAR(10) PRIMARY KEY, name VARCHAR(100), price DOUBLE)");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS trades (id INT PRIMARY KEY, traderId INT, symbol VARCHAR(10), quantity INT, price DOUBLE, type VARCHAR(4), timestamp VARCHAR(50))");
                st.executeUpdate("CREATE TABLE IF NOT EXISTS portfolios (traderId INT, symbol VARCHAR(10), qty INT, PRIMARY KEY (traderId, symbol))");
                System.out.println("[DB] Ensured tables exist.");
            } catch (SQLException e) {
                System.err.println("[DB] Could not create tables: " + e.getMessage());
            }
        }
    }

    /*************************************************************************
     * DAO Classes (Step 5: Classes for DB operations)
     *
     * Each DAO attempts to use JDBC when available (prepared statements),
     * otherwise it uses fallback in-memory + CSV persistence.
     *************************************************************************/
    public static abstract class BaseDAO {
        protected boolean hasJdbc() { return DBConnection.getConnection() != null; }
        // helper to write fallback CSVs
        protected void appendLineToFile(String fileName, String line) {
            try (FileWriter fw = new FileWriter(fileName, true); BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(line);
                bw.newLine();
            } catch (IOException e) {
                System.err.println("[IO] Write failed: " + e.getMessage());
            }
        }
    }

    public static class UserDAO extends BaseDAO {
        // fallback in-memory store
        private static Map<Integer, User> fallbackUsers = new HashMap<>();
        private static AtomicInteger idGen = new AtomicInteger(1);

        public UserDAO() {}

        public int addUser(String name, String email, String role) throws DatabaseException {
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try {
                    int id = idGen.getAndIncrement();
                    try (PreparedStatement ps = c.prepareStatement("INSERT INTO users (id,name,email,role) VALUES (?,?,?,?)")) {
                        ps.setInt(1, id);
                        ps.setString(2, name);
                        ps.setString(3, email);
                        ps.setString(4, role);
                        ps.executeUpdate();
                    }
                    return id;
                } catch (SQLException e) { throw new DatabaseException("Add user failed", e); }
            } else {
                int id = idGen.getAndIncrement();
                User u = role.equalsIgnoreCase("admin") ? new Admin(id, name, email) : new Trader(id, name, email);
                fallbackUsers.put(id, u);
                appendLineToFile("users_fallback.csv", id + "," + name + "," + email + "," + role);
                return id;
            }
        }

        public User getUser(int id) throws DatabaseException {
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try (PreparedStatement ps = c.prepareStatement("SELECT name,email,role FROM users WHERE id=?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString(1), email = rs.getString(2), role = rs.getString(3);
                            return role.equalsIgnoreCase("admin") ? new Admin(id, name, email) : new Trader(id, name, email);
                        } else return null;
                    }
                } catch (SQLException e) { throw new DatabaseException("Get user failed", e); }
            } else {
                return fallbackUsers.get(id);
            }
        }

        public List<User> getAllUsers() throws DatabaseException {
            List<User> list = new ArrayList<>();
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT id,name,email,role FROM users")) {
                    while (rs.next()) {
                        int id = rs.getInt(1);
                        String name = rs.getString(2), email = rs.getString(3), role = rs.getString(4);
                        list.add(role.equalsIgnoreCase("admin") ? new Admin(id, name, email) : new Trader(id, name, email));
                    }
                } catch (SQLException e) { throw new DatabaseException("Get all users failed", e); }
            } else {
                list.addAll(fallbackUsers.values());
            }
            return list;
        }

        // Delete, update etc can be added similarly.
    }

    public static class StockDAO extends BaseDAO {
        // fallback store
        private static Map<String, Stock> fallbackStocks = new HashMap<>();

        public StockDAO() {}

        public void addOrUpdateStock(Stock s) throws DatabaseException {
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try (PreparedStatement ps = c.prepareStatement("REPLACE INTO stocks (symbol,name,price) VALUES (?,?,?)")) {
                    ps.setString(1, s.getSymbol());
                    ps.setString(2, s.getName());
                    ps.setDouble(3, s.getPrice());
                    ps.executeUpdate();
                } catch (SQLException e) { throw new DatabaseException("Add/update stock failed", e); }
            } else {
                fallbackStocks.put(s.getSymbol(), s);
                appendLineToFile("stocks_fallback.csv", s.getSymbol() + "," + s.getName() + "," + s.getPrice());
            }
        }

        public Stock getStock(String symbol) throws DatabaseException {
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try (PreparedStatement ps = c.prepareStatement("SELECT name,price FROM stocks WHERE symbol=?")) {
                    ps.setString(1, symbol);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return new Stock(symbol, rs.getString(1), rs.getDouble(2));
                        }
                    }
                } catch (SQLException e) { throw new DatabaseException("Get stock failed", e); }
            } else {
                return fallbackStocks.get(symbol);
            }
            return null;
        }

        public List<Stock> getAllStocks() throws DatabaseException {
            List<Stock> list = new ArrayList<>();
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT symbol,name,price FROM stocks")) {
                    while (rs.next()) {
                        list.add(new Stock(rs.getString(1), rs.getString(2), rs.getDouble(3)));
                    }
                } catch (SQLException e) { throw new DatabaseException("Get all stocks failed", e); }
            } else {
                list.addAll(fallbackStocks.values());
            }
            return list;
        }

        public void updatePrice(String symbol, double newPrice) throws DatabaseException {
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE stocks SET price=? WHERE symbol=?")) {
                    ps.setDouble(1, newPrice);
                    ps.setString(2, symbol);
                    ps.executeUpdate();
                } catch (SQLException e) { throw new DatabaseException("Update price failed", e); }
            } else {
                Stock s = fallbackStocks.get(symbol);
                if (s != null) s.setPrice(newPrice);
                appendLineToFile("stocks_fallback.csv", symbol + ",<updated>," + newPrice);
            }
        }

        // For seeding in fallback mode:
        public void seedIfEmpty() {
            if (!hasJdbc() && fallbackStocks.isEmpty()) {
                fallbackStocks.put("ABC", new Stock("ABC", "ABC Corporation", 120.00));
                fallbackStocks.put("XYZ", new Stock("XYZ", "XYZ Limited", 45.50));
                fallbackStocks.put("TCS", new Stock("TCS", "TCS Ltd", 3500.00));
            }
        }
    }

    public static class TradeDAO extends BaseDAO {
        private static List<Trade> fallbackTrades = new ArrayList<>();
        private static AtomicInteger tradeIdGenerator = new AtomicInteger(1);

        public TradeDAO() {}

        public int addTrade(Trade t) throws DatabaseException {
            Connection c = DBConnection.getConnection();
            if (c != null) {
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO trades (id,traderId,symbol,quantity,price,type,timestamp) VALUES (?,?,?,?,?,?,?)")) {
                    ps.setInt(1, t.id);
                    ps.setInt(2, t.traderId);
                    ps.setString(3, t.symbol);
                    ps.setInt(4, t.quantity);
                    ps.setDouble(5, t.price);
                    ps.setString(6, t.type);
                    ps.setString(7, t.timestamp.toString());
                    ps.executeUpdate();
                    return t.id;
                } catch (SQLException e) { throw new DatabaseException("Add trade failed", e); }
            } else {
                t.id = tradeIdGenerator.getAndIncrement();
                fallbackTrades.add(t);
                appendLineToFile("trades_fallback.csv", t.toCSV());
                return t.id;
            }
        }

        public List<Trade> getTradesForTrader(int traderId) throws DatabaseException {
            Connection c = DBConnection.getConnection();
            List<Trade> res = new ArrayList<>();
            if (c != null) {
                try (PreparedStatement ps = c.prepareStatement("SELECT id,traderId,symbol,quantity,price,type,timestamp FROM trades WHERE traderId=?")) {
                    ps.setInt(1, traderId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Trade t = new Trade(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getDouble(5), rs.getString(6));
                            res.add(t);
                        }
                    }
                } catch (SQLException e) { throw new DatabaseException("Get trades failed", e); }
            } else {
                for (Trade t : fallbackTrades) if (t.getTraderId() == traderId) res.add(t);
            }
            return res;
        }
    }

    /*************************************************************************
     * Services (Step 3 & 4: Collections + Multithreading + Synchronization)
     * - TradingService implements TradeOperations
     * - PortfolioService manages portfolios
     *************************************************************************/
    public static class PortfolioService {
        // In-memory portfolio store per traderId
        private static Map<Integer, Portfolio> portfolios = new HashMap<>();

        public Portfolio getPortfolio(int traderId) {
            return portfolios.computeIfAbsent(traderId, k -> new Portfolio());
        }

        // synchronized method for thread-safe portfolio updates — Step 4 (synchronization)
        public synchronized void updatePortfolioOnBuy(int traderId, String symbol, int qty) {
            getPortfolio(traderId).add(symbol, qty);
        }

        public synchronized void updatePortfolioOnSell(int traderId, String symbol, int qty) {
            getPortfolio(traderId).remove(symbol, qty);
        }
    }

    public static class TradingService implements TradeOperations {
        private StockDAO stockDAO = new StockDAO();
        private TradeDAO tradeDAO = new TradeDAO();
        private PortfolioService portfolioService = new PortfolioService();

        // Simple in-memory account cash balances for demo
        private Map<Integer, Double> cashBalances = new HashMap<>();

        public TradingService() {
            // seed sample balances
            cashBalances.put(1, 100000.0);
            cashBalances.put(2, 50000.0);
        }

        @Override
        public synchronized void buyStock(int traderId, String symbol, int quantity) throws InvalidTradeException, DatabaseException {
            if (quantity <= 0) throw new InvalidTradeException("Quantity must be > 0");
            Stock s = stockDAO.getStock(symbol);
            if (s == null) throw new InvalidTradeException("Stock not found: " + symbol);
            double cost = s.getPrice() * quantity;
            double balance = cashBalances.getOrDefault(traderId, 0.0);
            if (balance < cost) throw new InvalidTradeException("Insufficient funds: required " + cost + " available " + balance);
            // debit
            cashBalances.put(traderId, balance - cost);
            // update portfolio
            portfolioService.updatePortfolioOnBuy(traderId, symbol, quantity);
            // record trade
            Trade t = new Trade(0, traderId, symbol, quantity, s.getPrice(), "BUY");
            tradeDAO.addTrade(t);
            System.out.println("[TRADE] BUY: Trader " + traderId + " bought " + quantity + " of " + symbol + " at " + s.getPrice());
        }

        @Override
        public synchronized void sellStock(int traderId, String symbol, int quantity) throws InvalidTradeException, DatabaseException {
            if (quantity <= 0) throw new InvalidTradeException("Quantity must be > 0");
            Portfolio p = portfolioService.getPortfolio(traderId);
            int held = p.getQuantity(symbol);
            if (held < quantity) throw new InvalidTradeException("Not enough holdings to sell");
            Stock s = stockDAO.getStock(symbol);
            if (s == null) throw new InvalidTradeException("Stock not found: " + symbol);
            double proceeds = s.getPrice() * quantity;
            cashBalances.put(traderId, cashBalances.getOrDefault(traderId, 0.0) + proceeds);
            portfolioService.updatePortfolioOnSell(traderId, symbol, quantity);
            Trade t = new Trade(0, traderId, symbol, quantity, s.getPrice(), "SELL");
            tradeDAO.addTrade(t);
            System.out.println("[TRADE] SELL: Trader " + traderId + " sold " + quantity + " of " + symbol + " at " + s.getPrice());
        }

        public double getCashBalance(int traderId) { return cashBalances.getOrDefault(traderId, 0.0); }
    }

    /*************************************************************************
     * Market Update Thread (Step 4: Multithreading)
     * - Simulates real-time market price updates in the background.
     *************************************************************************/
    public static class MarketUpdateThread extends Thread {
        private volatile boolean running = true;
        private StockDAO stockDAO;
        private Random rnd = new Random();

        public MarketUpdateThread(StockDAO dao) {
            this.stockDAO = dao;
            setName("MarketUpdateThread");
        }

        @Override
        public void run() {
            // runs in background and updates stock prices every 2 seconds
            while (running) {
                try {
                    List<Stock> stocks = stockDAO.getAllStocks();
                    for (Stock s : stocks) {
                        double changePct = (rnd.nextDouble() - 0.5) * 0.02; // ±1% change
                        double newPrice = Math.max(0.01, s.getPrice() * (1 + changePct));
                        s.setPrice(round(newPrice, 2));
                        stockDAO.updatePrice(s.getSymbol(), s.getPrice());
                    }
                    // Sleep to simulate periodic updates
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    running = false;
                } catch (DatabaseException de) {
                    System.err.println("[MarketThread] DB error: " + de.getMessage());
                }
            }
        }

        public void shutdown() { running = false; interrupt(); }
        private double round(double v, int places) {
            double factor = Math.pow(10, places);
            return Math.round(v * factor) / factor;
        }
    }

    /*************************************************************************
     * Simple Swing UI (Step 7: Partial GUI)
     * - LoginFrame, AdminDashboard, TraderDashboard
     *
     * Note: GUI is minimal but demonstrates required screens and connects
     * to services to show functionality.
     *************************************************************************/
    public static class LoginFrame extends JFrame {
        private UserDAO userDAO = new UserDAO();
        public LoginFrame() {
            setTitle("Online Stock Trading - Login");
            setSize(400, 220);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            JPanel panel = new JPanel(new GridLayout(4,2,6,6));
            panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            panel.add(new JLabel("Name:"));
            JTextField nameField = new JTextField("Demo Trader");
            panel.add(nameField);
            panel.add(new JLabel("Email:"));
            JTextField emailField = new JTextField("trader@example.com");
            panel.add(emailField);

            panel.add(new JLabel("Role:"));
            JComboBox<String> roleBox = new JComboBox<>(new String[]{"Trader","Admin"});
            panel.add(roleBox);

            JButton loginBtn = new JButton("Create & Login");
            panel.add(new JLabel());
            panel.add(loginBtn);

            add(panel, BorderLayout.CENTER);

            loginBtn.addActionListener(e -> {
                String name = nameField.getText().trim();
                String email = emailField.getText().trim();
                String role = (String) roleBox.getSelectedItem();
                if (name.isEmpty() || email.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Enter name and email");
                    return;
                }
                try {
                    int id = userDAO.addUser(name, email, role);
                    User user = userDAO.getUser(id);
                    if (user instanceof Admin) {
                        SwingUtilities.invokeLater(() -> {
                            new AdminDashboard((Admin)user).setVisible(true);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            new TraderDashboard((Trader)user).setVisible(true);
                        });
                    }
                    this.dispose();
                } catch (DatabaseException ex) {
                    JOptionPane.showMessageDialog(this, "DB Error: " + ex.getMessage());
                }
            });
        }
    }

    public static class AdminDashboard extends JFrame {
        private User adminUser;
        private UserDAO userDAO = new UserDAO();
        private StockDAO stockDAO = new StockDAO();
        public AdminDashboard(Admin admin) {
            this.adminUser = admin;
            setTitle("Admin Dashboard - " + admin.getName());
            setSize(800,600);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            // Top panel with actions
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addUserBtn = new JButton("Add User");
            JButton loadUsersBtn = new JButton("Load Users");
            JButton addStockBtn = new JButton("Add/Seed Stocks");
            top.add(new JLabel("Admin Actions: "));
            top.add(addUserBtn);
            top.add(loadUsersBtn);
            top.add(addStockBtn);
            add(top, BorderLayout.NORTH);

            // center: user table
            String[] cols = {"ID","Name","Email","Role"};
            DefaultTableModel model = new DefaultTableModel(cols,0);
            JTable table = new JTable(model);
            add(new JScrollPane(table), BorderLayout.CENTER);

            addUserBtn.addActionListener(e -> {
                String name = JOptionPane.showInputDialog(this, "User name:");
                String email = JOptionPane.showInputDialog(this, "User email:");
                String role = JOptionPane.showInputDialog(this, "Role (Admin/Trader):", "Trader");
                if (name==null || email==null || role==null) return;
                try {
                    userDAO.addUser(name, email, role);
                    JOptionPane.showMessageDialog(this, "User added");
                } catch (DatabaseException ex) {
                    JOptionPane.showMessageDialog(this, "Add failed: " + ex.getMessage());
                }
            });

            loadUsersBtn.addActionListener(e -> {
                try {
                    List<User> users = userDAO.getAllUsers();
                    model.setRowCount(0);
                    for (User u : users) model.addRow(new Object[]{u.getId(), u.getName(), u.getEmail(), (u instanceof Admin ? "Admin":"Trader")});
                } catch (DatabaseException ex) {
                    JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
                }
            });

            addStockBtn.addActionListener(e -> {
                stockDAO.seedIfEmpty();
                try {
                    for (Stock s : stockDAO.getAllStocks()) stockDAO.addOrUpdateStock(s); // ensure persisted
                    JOptionPane.showMessageDialog(this, "Seeded stock list and persisted (fallback or JDBC)");
                } catch (DatabaseException ex) {
                    JOptionPane.showMessageDialog(this, "Stock seed failed: " + ex.getMessage());
                }
            });

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        }
    }

    public static class TraderDashboard extends JFrame {
        private Trader trader;
        private StockDAO stockDAO = new StockDAO();
        private TradingService tradingService = new TradingService();
        private PortfolioService portfolioService = new PortfolioService();
        private TradeDAO tradeDAO = new TradeDAO();

        private DefaultTableModel stockModel;
        private DefaultTableModel portfolioModel;
        private DefaultTableModel tradeModel;

        public TraderDashboard(Trader trader) {
            this.trader = trader;
            setTitle("Trader Dashboard - " + trader.getName());
            setSize(900,700);
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            // north: account info and actions
            JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
            north.add(new JLabel("Trader: " + trader.getName()));
            JButton refreshBtn = new JButton("Refresh Data");
            north.add(refreshBtn);
            add(north, BorderLayout.NORTH);

            // center split: stocks and portfolio/trade history
            JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
            // left: stocks
            JPanel left = new JPanel(new BorderLayout());
            stockModel = new DefaultTableModel(new String[]{"Symbol","Name","Price"},0);
            JTable stockTable = new JTable(stockModel);
            left.add(new JLabel("Available Stocks"), BorderLayout.NORTH);
            left.add(new JScrollPane(stockTable), BorderLayout.CENTER);
            JPanel tradePanel = new JPanel(new FlowLayout());
            JTextField qtyField = new JTextField("1",5);
            JButton buyBtn = new JButton("Buy");
            JButton sellBtn = new JButton("Sell");
            tradePanel.add(new JLabel("Qty:")); tradePanel.add(qtyField);
            tradePanel.add(buyBtn); tradePanel.add(sellBtn);
            left.add(tradePanel, BorderLayout.SOUTH);

            // right: portfolio & trades
            JPanel right = new JPanel(new BorderLayout());
            portfolioModel = new DefaultTableModel(new String[]{"Symbol","Qty"},0);
            JTable portfolioTable = new JTable(portfolioModel);
            right.add(new JLabel("Portfolio"), BorderLayout.NORTH);
            right.add(new JScrollPane(portfolioTable), BorderLayout.CENTER);

            tradeModel = new DefaultTableModel(new String[]{"Type","Symbol","Qty","Price"},0);
            JTable tradeTable = new JTable(tradeModel);
            right.add(new JLabel("Trade History"), BorderLayout.SOUTH);
            right.add(new JScrollPane(tradeTable), BorderLayout.SOUTH);

            center.setLeftComponent(left);
            center.setRightComponent(right);
            center.setDividerLocation(500);
            add(center, BorderLayout.CENTER);

            // actions
            refreshBtn.addActionListener(e -> refreshAll());
            buyBtn.addActionListener(e -> {
                int row = stockTable.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(this, "Select a stock"); return; }
                String symbol = (String) stockModel.getValueAt(row,0);
                try {
                    int qty = Integer.parseInt(qtyField.getText().trim());
                    tradingService.buyStock(trader.getId(), symbol, qty);
                    JOptionPane.showMessageDialog(this, "Bought " + qty + " of " + symbol);
                    refreshAll();
                } catch (NumberFormatException nfe) { JOptionPane.showMessageDialog(this, "Enter integer qty"); }
                catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage()); }
            });
            sellBtn.addActionListener(e -> {
                int row = stockTable.getSelectedRow();
                if (row < 0) { JOptionPane.showMessageDialog(this, "Select a stock"); return; }
                String symbol = (String) stockModel.getValueAt(row,0);
                try {
                    int qty = Integer.parseInt(qtyField.getText().trim());
                    tradingService.sellStock(trader.getId(), symbol, qty);
                    JOptionPane.showMessageDialog(this, "Sold " + qty + " of " + symbol);
                    refreshAll();
                } catch (NumberFormatException nfe) { JOptionPane.showMessageDialog(this, "Enter integer qty"); }
                catch (Exception ex) { JOptionPane.showMessageDialog(this, ex.getMessage()); }
            });

            refreshAll();

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        }

        private void refreshAll() {
            try {
                // stocks
                List<Stock> stocks = stockDAO.getAllStocks();
                stockModel.setRowCount(0);
                for (Stock s : stocks) stockModel.addRow(new Object[]{s.getSymbol(), s.getName(), s.getPrice()});
                // portfolio
                Portfolio p = portfolioService.getPortfolio(trader.getId());
                portfolioModel.setRowCount(0);
                for (Map.Entry<String,Integer> en : p.getHoldings().entrySet()) portfolioModel.addRow(new Object[]{en.getKey(), en.getValue()});
                // trades
                List<Trade> trades = tradeDAO.getTradesForTrader(trader.getId());
                tradeModel.setRowCount(0);
                for (Trade t : trades) tradeModel.addRow(new Object[]{t.getType(), t.getSymbol(), t.getQuantity(), t.getPrice()});
            } catch (DatabaseException e) {
                JOptionPane.showMessageDialog(this, "Refresh error: " + e.getMessage());
            }
        }
    }

    /*************************************************************************
     * Main: seeds data, starts market thread and opens login UI.
     *
     * This demonstrates:
     *  - seeding stocks (StockDAO.seedIfEmpty)
     *  - DBConnection.ensureTablesExist (JDBC creation attempt)
     *  - starting background MarketUpdateThread
     *************************************************************************/
    public static void main(String[] args) {
        // Try to set up JDBC and create tables (Step 6). If JDBC not available, fallback will be used.
        DBConnection.ensureTablesExist();

        // Seed initial data
        StockDAO stockDAO = new StockDAO();
        stockDAO.seedIfEmpty();
        try {
            for (Stock s : stockDAO.getAllStocks()) stockDAO.addOrUpdateStock(s);
        } catch (DatabaseException e) {
            System.err.println("[Main] Stock seed persist failed: " + e.getMessage());
        }

        // Start market update thread (Step 4: multithreading)
        MarketUpdateThread marketThread = new MarketUpdateThread(stockDAO);
        marketThread.setDaemon(true);
        marketThread.start();

        // Start GUI on EDT (Step 7: GUI)
        SwingUtilities.invokeLater(() -> {
            new LoginFrame().setVisible(true);
        });

        // Add shutdown hook to ensure thread stops
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            marketThread.shutdown();
            System.out.println("[Main] Shutdown complete.");
        }));
    }
}