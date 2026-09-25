package com.fortydash.repository;
import com.fortydash.domain.model.Product;
import com.fortydash.infrastructure.db.DatabaseManager;
import java.sql.*;
import java.util.*;

public class ProductRepository {
    public Optional<Product> findByBarcode(String barcode) throws SQLException {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Product(rs.getString("barcode"), rs.getString("name"), rs.getBigDecimal("price"), rs.getBigDecimal("cost"), rs.getInt("quantity")));
                }
            }
        }
        return Optional.empty();
    }
    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection(); ResultSet rs = c.createStatement().executeQuery("SELECT * FROM products")) {
            while (rs.next()) {
                list.add(new Product(rs.getString("barcode"), rs.getString("name"), rs.getBigDecimal("price"), rs.getBigDecimal("cost"), rs.getInt("quantity")));
            }
        }
        return list;
    }
    public void save(Product p) throws SQLException {
        String sql = "INSERT OR REPLACE INTO products VALUES(?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getBarcode());
            ps.setString(2, p.getName());
            ps.setBigDecimal(3, p.getPrice());
            ps.setBigDecimal(4, p.getCost());
            ps.setInt(5, p.getQuantity());
            ps.executeUpdate();
        }
    }
    public void updateStock(Connection c, String barcode, int qty) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET quantity = quantity - ? WHERE barcode = ? AND quantity >= ?")) {
            ps.setInt(1, qty); ps.setString(2, barcode); ps.setInt(3, qty);
            if (ps.executeUpdate() == 0) throw new SQLException("الكمية غير كافية بالمخزن");
        }
    }
    public void restoreStock(Connection c, String barcode, int qty) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE products SET quantity = quantity + ? WHERE barcode = ?")) {
            ps.setInt(1, qty); ps.setString(2, barcode);
            ps.executeUpdate();
        }
    }
}