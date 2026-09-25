package com.fortydash.service;
import com.fortydash.domain.model.OrderItem;
import com.fortydash.domain.model.Product;
import com.fortydash.infrastructure.db.DatabaseManager;
import com.fortydash.infrastructure.hardware.EscPosService;
import com.fortydash.infrastructure.logging.AuditLogger;
import com.fortydash.repository.ProductRepository;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

public class SalesService {
    private final ProductRepository productRepo = new ProductRepository();

    public int checkout(List<OrderItem> cart, String cashier) throws SQLException {
        if (cart == null || cart.isEmpty()) throw new IllegalArgumentException("الفاتورة فارغة");
        BigDecimal total = cart.stream().map(OrderItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal profit = cart.stream().map(i -> i.getPrice().subtract(i.getCost()).multiply(BigDecimal.valueOf(i.getQty()))).reduce(BigDecimal.ZERO, BigDecimal::add);
        String branch = DatabaseManager.getProperty("branch.id", "BRANCH-MAIN");

        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement po = c.prepareStatement("INSERT INTO orders (date, total, profit, cashier, branch_id) VALUES (?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
                    po.setString(1, LocalDateTime.now().toString()); po.setBigDecimal(2, total); po.setBigDecimal(3, profit); po.setString(4, cashier); po.setString(5, branch);
                    po.executeUpdate();
                    ResultSet rs = po.getGeneratedKeys();
                    rs.next(); orderId = rs.getInt(1);
                }
                for (OrderItem itm : cart) {
                    try (PreparedStatement pi = c.prepareStatement("INSERT INTO order_items (order_id, barcode, name, qty, total) VALUES (?,?,?,?,?)")) {
                        pi.setInt(1, orderId); pi.setString(2, itm.getBarcode()); pi.setString(3, itm.getName()); pi.setInt(4, itm.getQty()); pi.setBigDecimal(5, itm.getTotal());
                        pi.executeUpdate();
                    }
                    productRepo.updateStock(c, itm.getBarcode(), itm.getQty());
                }
                c.commit();
                AuditLogger.logSale(orderId, cashier, total.toString(), branch);
                EscPosService.printReceiptDirect(("40DASH RECEIPT #" + orderId + "\nTOTAL: " + total + " EGP\n\n").getBytes());
                return orderId;
            } catch (Exception ex) {
                c.rollback();
                AuditLogger.logError("فشل إتمام الفاتورة", ex);
                throw new SQLException(ex.getMessage());
            } finally { c.setAutoCommit(true); }
        }
    }

    public void refund(String barcode, int qty, String cashier) throws SQLException {
        Product p = productRepo.findByBarcode(barcode).orElseThrow(() -> new SQLException("المنتج غير موجود"));
        String branch = DatabaseManager.getProperty("branch.id", "BRANCH-MAIN");
        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                productRepo.restoreStock(c, barcode, qty);
                BigDecimal total = p.getPrice().multiply(BigDecimal.valueOf(qty)).negate();
                BigDecimal profit = p.getPrice().subtract(p.getCost()).multiply(BigDecimal.valueOf(qty)).negate();
                try (PreparedStatement po = c.prepareStatement("INSERT INTO orders (date, total, profit, cashier, branch_id) VALUES (?,?,?,?,?)")) {
                    po.setString(1, LocalDateTime.now().toString()); po.setBigDecimal(2, total); po.setBigDecimal(3, profit); po.setString(4, cashier); po.setString(5, branch);
                    po.executeUpdate();
                }
                c.commit();
                AuditLogger.logRefund(barcode, qty, cashier, branch);
            } catch (Exception ex) {
                c.rollback();
                AuditLogger.logError("فشل تسجيل المرتجع", ex);
                throw new SQLException(ex.getMessage());
            } finally { c.setAutoCommit(true); }
        }
    }
}