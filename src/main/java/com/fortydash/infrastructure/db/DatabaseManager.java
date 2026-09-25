package com.fortydash.infrastructure.db;
import com.fortydash.infrastructure.security.PasswordHasher;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

public class DatabaseManager {
    private static final Properties props = new Properties();
    private static final HikariDataSource dataSource;

    static {
        loadProperties();
        HikariConfig cfg = new HikariConfig();
        String mode = props.getProperty("db.mode", "sqlite").trim().toLowerCase();
        if ("postgres".equals(mode)) {
            cfg.setDriverClassName("org.postgresql.Driver");
            cfg.setJdbcUrl(props.getProperty("postgres.url"));
            cfg.setUsername(props.getProperty("postgres.user"));
            cfg.setPassword(props.getProperty("postgres.password"));
            cfg.setMaximumPoolSize(20);
        } else {
            cfg.setDriverClassName("org.sqlite.JDBC");
            cfg.setJdbcUrl(props.getProperty("sqlite.url", "jdbc:sqlite:40dash.db"));
            cfg.setMaximumPoolSize(10);
        }
        cfg.setConnectionTimeout(10000);
        dataSource = new HikariDataSource(cfg);
        initTables();
    }

    private static void loadProperties() {
        File f = new File("config/application.properties");
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) { props.load(in); } catch (Exception ignored) {}
        }
    }

    public static Connection getConnection() throws SQLException { return dataSource.getConnection(); }
    public static String getProperty(String k, String d) { return props.getProperty(k, d); }
    public static boolean isNetworkPrinterEnabled() {
        return "true".equalsIgnoreCase(props.getProperty("printer.network.enabled", "false"));
    }

    private static void initTables() {
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, role TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS products (barcode TEXT PRIMARY KEY, name TEXT, price DECIMAL(10,2), cost DECIMAL(10,2), quantity INTEGER)");
            s.execute("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, total DECIMAL(10,2), profit DECIMAL(10,2), cashier TEXT, branch_id TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, barcode TEXT, name TEXT, qty INTEGER, total DECIMAL(10,2))");
            s.execute("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, title TEXT, amount DECIMAL(10,2), user TEXT, branch_id TEXT)");

            try (PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username = 'admin'")) {
                if (!ps.executeQuery().next()) {
                    try (PreparedStatement ins = c.prepareStatement("INSERT INTO users VALUES ('admin', ?, 'ADMIN')")) {
                        ins.setString(1, PasswordHasher.hash("admin"));
                        ins.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}