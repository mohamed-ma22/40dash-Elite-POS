package com.fortydash.infrastructure.logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditLogger {
    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");
    private static final Logger errorLog = LoggerFactory.getLogger("ERROR");

    public static void logSale(int orderId, String cashier, String total, String branch) {
        auditLog.info("TRANSACTION | OrderID: {} | Cashier: {} | Amount: {} EGP | Branch: {}", orderId, cashier, total, branch);
    }
    public static void logRefund(String barcode, int qty, String cashier, String branch) {
        auditLog.warn("REFUND | Barcode: {} | Qty: {} | Cashier: {} | Branch: {}", barcode, qty, cashier, branch);
    }
    public static void logSecurity(String event, String user, String status) {
        auditLog.info("SECURITY | Event: {} | User: {} | Status: {}", event, user, status);
    }
    public static void logError(String message, Throwable t) {
        errorLog.error(message, t);
    }
}