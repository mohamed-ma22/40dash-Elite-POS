package com.fortydash.infrastructure.hardware;
import com.fortydash.infrastructure.db.DatabaseManager;
import com.fortydash.infrastructure.logging.AuditLogger;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class EscPosService {
    private static final byte[] INIT_PRINTER = {0x1B, 0x40};
    private static final byte[] OPEN_DRAWER  = {0x1B, 0x70, 0x00, 0x19, (byte) 0xFA};
    private static final byte[] CUT_PAPER    = {0x1D, 0x56, 0x41, 0x10};

    public static void printReceiptDirect(byte[] receiptData) {
        if (!DatabaseManager.isNetworkPrinterEnabled()) return;
        String ip = DatabaseManager.getProperty("printer.network.ip", "192.168.1.200");
        int port = Integer.parseInt(DatabaseManager.getProperty("printer.network.port", "9100"));
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 2000);
            try (OutputStream out = socket.getOutputStream()) {
                out.write(INIT_PRINTER);
                if ("true".equalsIgnoreCase(DatabaseManager.getProperty("printer.drawer.auto_open", "true"))) {
                    out.write(OPEN_DRAWER);
                }
                out.write(receiptData);
                out.write(CUT_PAPER);
                out.flush();
            }
        } catch (Exception ex) {
            AuditLogger.logError("تعذر الاتصال بطابعة الشبكة المباشرة", ex);
        }
    }
}