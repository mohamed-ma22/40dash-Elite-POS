package com.fortydash.repository;
import com.fortydash.domain.model.*;
import com.fortydash.infrastructure.db.DatabaseManager;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class AppRepository {
    public void saveExpense(Expense e) throws SQLException {
        String branch = DatabaseManager.getProperty("branch.id", "BRANCH-MAIN");
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT INTO expenses (date, title, amount, user, branch_id) VALUES (?,?,?,?,?)")) {
            ps.setString(1, e.getDate()); ps.setString(2, e.getTitle()); ps.setBigDecimal(3, e.getAmount()); ps.setString(4, e.getUser()); ps.setString(5, branch);
            ps.executeUpdate();
        }
    }
    public List<Expense> findAllExpenses() throws SQLException {
        List<Expense> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT * FROM expenses ORDER BY id DESC")) {
            while (rs.next()) list.add(new Expense(rs.getString("date"), rs.getString("title"), rs.getBigDecimal("amount"), rs.getString("user")));
        }
        return list;
    }
    public List<UserData> findAllUsers() throws SQLException {
        List<UserData> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT username, role FROM users")) {
            while (rs.next()) list.add(new UserData(rs.getString(1), rs.getString(2)));
        }
        return list;
    }
    public void saveUser(String u, String hash, String role) throws SQLException {
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO users VALUES(?,?,?)")) {
            ps.setString(1, u); ps.setString(2, hash); ps.setString(3, role);
            ps.executeUpdate();
        }
    }
    public BigDecimal[] getFinancialStats() throws SQLException {
        BigDecimal sales = BigDecimal.ZERO, profit = BigDecimal.ZERO, exp = BigDecimal.ZERO;
        try (Connection c = DatabaseManager.getConnection()) {
            ResultSet r1 = c.createStatement().executeQuery("SELECT SUM(total), SUM(profit) FROM orders");
            if (r1.next()) {
                if (r1.getBigDecimal(1) != null) sales = r1.getBigDecimal(1);
                if (r1.getBigDecimal(2) != null) profit = r1.getBigDecimal(2);
            }
            ResultSet r2 = c.createStatement().executeQuery("SELECT SUM(amount) FROM expenses");
            if (r2.next() && r2.getBigDecimal(1) != null) exp = r2.getBigDecimal(1);
        }
        return new BigDecimal[]{sales, exp, profit.subtract(exp)};
    }
    public BigDecimal[] getShiftStats(String user, String datePrefix) throws SQLException {
        BigDecimal sales = BigDecimal.ZERO, exp = BigDecimal.ZERO;
        try (Connection c = DatabaseManager.getConnection()) {
            PreparedStatement ps1 = c.prepareStatement("SELECT SUM(total) FROM orders WHERE cashier=? AND date LIKE ?");
            ps1.setString(1, user); ps1.setString(2, datePrefix + "%");
            ResultSet r1 = ps1.executeQuery();
            if (r1.next() && r1.getBigDecimal(1) != null) sales = r1.getBigDecimal(1);

            PreparedStatement ps2 = c.prepareStatement("SELECT SUM(amount) FROM expenses WHERE user=? AND date LIKE ?");
            ps2.setString(1, user); ps2.setString(2, datePrefix + "%");
            ResultSet r2 = ps2.executeQuery();
            if (r2.next() && r2.getBigDecimal(1) != null) exp = r2.getBigDecimal(1);
        }
        return new BigDecimal[]{sales, exp, sales.subtract(exp)};
    }
}