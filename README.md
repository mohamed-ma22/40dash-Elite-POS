# 🛒 40DASH POS - Enterprise Edition (v2.0.0)

<p align="center">
  <img src="src/main/resources/assets/logo.png" alt="40DASH Logo" width="220"/>
</p>

<p align="center">
  <b>منظومة نقاط بيع وإدارة مخازن مؤسسية متقدمة مبنية على Clean Architecture ومحصنة بأنظمة تراخيص ذكية.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/JavaFX-21-blue?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Database-SQLite%20%7C%20PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Security-BCrypt%20%2B%20HWID-green?style=for-the-badge" />
</p>

---

## 👨‍💻 Developer & Ownership
* **Lead Engineer:** Mohamed Ashraf
* **Phone / Support:** `01080485595`
* **LinkedIn:** [Mohamed Ashraf Profile](https://www.linkedin.com/in/mohamed-ashraf-3251b22a4/)
* **Copyright:** © 2026 All Rights Reserved to Mohamed Ashraf.

---

## 📸 System Showcase & Screenshots (معرض واجهات المنظومة)

### 1. منظومة الأمان والترخيص الرقمي (Licensing & Auth)
| نافذة تفعيل الترخيص وبصمة العتاد | شاشة تسجيل دخول الموظفين |
| :---: | :---: |
| ![License Activation](docs/screenshots/activation.png) | ![Employee Login](docs/screenshots/login.png) |

---

### 2. نقطة البيع وإدارة المخازن (POS & Inventory)
| شاشة الكاشير والتحكم في الفواتير والمرتجعات | إدارة المنتجات وتكاليف المخزون |
| :---: | :---: |
| ![Cashier Desk](docs/screenshots/cashier.png) | ![Inventory Management](docs/screenshots/inventory.png) |

---

### 3. الإدارة المالية والرقابة والتحليلات (Finance & Analytics)
| لوحة مؤشرات الأداء المالي (Dashboard) | تسجيل وتتبع المصروفات التشغيلية |
| :---: | :---: |
| ![Financial Dashboard](docs/screenshots/dashboard.png) | ![Expenses Tracker](docs/screenshots/expenses.png) |

---

### 4. إدارة صلاحيات الموظفين (Role-Based Access Control)
<p align="center">
  <img src="docs/screenshots/roles.png" alt="RBAC Permissions" width="85%"/>
</p>

---

## 🚀 Key Features

* **Clean Architecture:** فصل كامل للمسؤوليات داخل طبقات معمارية مستقلة (`domain`, `repository`, `service`, `infrastructure`, `ui`).
* **Hybrid Database Engine:** دعم التبديل الديناميكي بين قاعدة بيانات محلية (SQLite) وسحابية (PostgreSQL) عبر HikariCP Connection Pooling.
* **HWID Hardware Licensing:** قفل الترخيص ببصمة عتاد الجهاز مع دعم مدد مرنة (Lifetime, 1 Year, 30-Day Trial).
* **Anti-Clock Tampering:** حماية استباقية توقف النظام عند اكتشاف أي تلاعب بساعة الجهاز للتحايل على فترات الاشتراك.
* **Thermal Printing (ESC/POS):** طباعة فواتير حرارية مباشرة ودعم فتح درج النقود تلقائياً بعد السداد.
* **ACID Sales Transactions & Refunds:** حسابات مالية بالغة الدقة باستخدام `BigDecimal` لمنع أي خطأ تقريب أو عجز مالي.
* **Audit Logging:** تسجيل كل تحركات الكاشير وتفاصيل الفواتير والشيفتات عبر `SLF4J` و `Logback`.

---

## 🏗️ Project Structure

```text
src/main/java/com/fortydash/
├── domain/            # Core business models (Product, OrderItem, Expense)
├── repository/        # Data access layer & SQL queries
├── service/           # Business rules, checkout & refund transactions
├── infrastructure/    # Hardware (ESC/POS), DB pooling, HWID licensing
└── ui/                # Modern JavaFX interface & desktop components
