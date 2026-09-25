package com.fortydash.infrastructure.security;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class LicenseService {
    private static final String SECRET_SALT = "40DASH_MOHAMED_ASHRAF_ENTERPRISE_SECURE_SALT_2026";
    private static final String LICENSE_FILE = "license.key";
    private static final String TIME_FILE = "config/.sys_sync";

    public enum Status { VALID, EXPIRED, NOT_ACTIVATED, CLOCK_TAMPERED }

    public static String getHWID() {
        String comp = System.getenv("COMPUTERNAME");
        String user = System.getProperty("user.name");
        if (comp == null) comp = "DEVICE";
        if (user == null) user = "USER";
        return (comp + "-" + user).toUpperCase().replaceAll("\\s+", "");
    }

    public static String generateKey(String hwid, String type, LocalDate expiryDate) {
        String expStr = expiryDate.format(DateTimeFormatter.BASIC_ISO_DATE); // YYYYMMDD
        String raw = hwid + "#" + type + "#" + expStr + "#" + SECRET_SALT;
        String sign = hash(raw).substring(0, 8);
        return "40D-" + type + "-" + expStr + "-" + sign;
    }

    public static Status checkStatus() {
        File file = new File(LICENSE_FILE);
        if (!file.exists()) return Status.NOT_ACTIVATED;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String key = br.readLine();
            if (key == null || !key.startsWith("40D-")) return Status.NOT_ACTIVATED;

            String[] parts = key.trim().split("-");
            if (parts.length != 4) return Status.NOT_ACTIVATED;

            String type = parts[1];
            String expStr = parts[2];
            String sign = parts[3];

            String expectedSign = hash(getHWID() + "#" + type + "#" + expStr + "#" + SECRET_SALT).substring(0, 8);
            if (!sign.equalsIgnoreCase(expectedSign)) return Status.NOT_ACTIVATED;

            LocalDate expiry = LocalDate.parse(expStr, DateTimeFormatter.BASIC_ISO_DATE);
            LocalDate now = LocalDate.now();

            // فحص التلاعب بساعة النظام (Anti-Clock Tampering)
            File tf = new File(TIME_FILE);
            if (tf.exists()) {
                try (BufferedReader tbr = new BufferedReader(new FileReader(tf))) {
                    String lastDateStr = tbr.readLine();
                    if (lastDateStr != null) {
                        LocalDate lastDate = LocalDate.parse(lastDateStr.trim(), DateTimeFormatter.BASIC_ISO_DATE);
                        if (now.isBefore(lastDate)) {
                            return Status.CLOCK_TAMPERED; // تم إرجاع التاريخ للخلف!
                        }
                    }
                }
            }
            // تحديث آخر تاريخ تشغيل
            tf.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(tf)) {
                fw.write(now.format(DateTimeFormatter.BASIC_ISO_DATE));
            }

            if (now.isAfter(expiry)) {
                return Status.EXPIRED;
            }

            return Status.VALID;
        } catch (Exception e) {
            return Status.NOT_ACTIVATED;
        }
    }

    public static boolean activate(String inputKey) {
        if (inputKey == null || !inputKey.startsWith("40D-")) return false;
        String[] parts = inputKey.trim().split("-");
        if (parts.length != 4) return false;

        String type = parts[1];
        String expStr = parts[2];
        String sign = parts[3];

        String expectedSign = hash(getHWID() + "#" + type + "#" + expStr + "#" + SECRET_SALT).substring(0, 8);
        if (sign.equalsIgnoreCase(expectedSign)) {
            try (FileWriter fw = new FileWriter(LICENSE_FILE)) {
                fw.write(inputKey.trim());
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public static String getLicenseInfo() {
        File file = new File(LICENSE_FILE);
        if (!file.exists()) return "غير مفعل";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String key = br.readLine();
            String[] parts = key.split("-");
            String type = parts[1];
            LocalDate exp = LocalDate.parse(parts[2], DateTimeFormatter.BASIC_ISO_DATE);
            long days = ChronoUnit.DAYS.between(LocalDate.now(), exp);
            if ("LIFE".equalsIgnoreCase(type)) return "ترخيص دائم (مدى الحياة)";
            return "ترخيص نشط (متبقي: " + days + " يوم حتى " + exp + ")";
        } catch (Exception e) {
            return "مفعل";
        }
    }

    private static String hash(String txt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(txt.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02X", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}