package com.fortydash.infrastructure.security;

import java.time.LocalDate;
import java.util.Scanner;

public class KeyGen {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("==================================================");
        System.out.println("   40DASH POS - مولد التراخيص الذكي (Mohamed Ashraf)  ");
        System.out.println("==================================================");
        System.out.print("1. أدخل كود البصمة (HWID) الخاص بالعميل: ");
        String hwid = sc.nextLine().trim();

        if (hwid.isEmpty()) {
            System.out.println("❌ البصمة فارغة!");
            return;
        }

        System.out.println("\nاختر نوع الترخيص:");
        System.out.println(" [1] ترخيص مدى الحياة (Lifetime)");
        System.out.println(" [2] اشتراك سنوي (سنة كاملة - 365 يوم)");
        System.out.println(" [3] فترة تجريبية (30 يوماً)");
        System.out.print("اختيارك (1-3): ");
        String choice = sc.nextLine().trim();

        String type = "YEAR";
        LocalDate expiry = LocalDate.now().plusYears(1);

        if ("1".equals(choice)) {
            type = "LIFE";
            expiry = LocalDate.now().plusYears(99);
        } else if ("3".equals(choice)) {
            type = "TRIL";
            expiry = LocalDate.now().plusDays(30);
        }

        String key = LicenseService.generateKey(hwid, type, expiry);

        System.out.println("\n==================================================");
        System.out.println("✅ تم توليد كود الترخيص بنجاح:");
        System.out.println("🔑  " + key + "  🔑");
        System.out.println("📅 تاريخ الانتهاء: " + expiry);
        System.out.println("==================================================");
    }
}