package com.fortydash.ui;

import com.fortydash.domain.model.*;
import com.fortydash.infrastructure.db.DatabaseManager;
import com.fortydash.infrastructure.security.LicenseService;
import com.fortydash.infrastructure.security.PasswordHasher;
import com.fortydash.repository.*;
import com.fortydash.service.SalesService;
import javafx.application.*;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.*;
import javafx.util.converter.IntegerStringConverter;

import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.print.PrinterJob;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainApp extends Application {
    private static String loggedUser = "";
    private static String loggedRole = "";
    private static final VBox notificationArea = new VBox(5.0);
    private static final ProductRepository productRepo = new ProductRepository();
    private static final AppRepository appRepo = new AppRepository();
    private static final SalesService salesService = new SalesService();

    public static final String DEV_NAME = "Mohamed Ashraf";
    public static final String DEV_PHONE = "01080485595";
    public static final String DEV_LINKEDIN = "https://www.linkedin.com/in/mohamed-ashraf-3251b22a4/";
    public static final String SYSTEM_NAME = "40DASH POS SYSTEMS";

    private static final String CSS = 
        ".root { -fx-font-family: 'Segoe UI Semibold'; -fx-base: #f1f5f9; }\n" +
        ".nav-bar { -fx-background-color: #0c1626; -fx-padding: 10 25; }\n" +
        ".tab-pane .tab-header-area .tab-header-background { -fx-background-color: #111e33; }\n" +
        ".tab { -fx-background-color: #1e293b; -fx-text-fill: white; -fx-padding: 12 25; }\n" +
        ".tab:selected { -fx-background-color: #0ea5e9; }\n" +
        ".button-primary { -fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n" +
        ".button-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n" +
        ".button-danger { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n" +
        ".card { -fx-background-color: white; -fx-background-radius: 16; -fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 8); }\n" +
        ".total-label { -fx-text-fill: #0c1626; -fx-font-size: 45px; -fx-font-weight: 900; }\n" +
        ".toast { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; -fx-font-size: 14px; }\n" +
        ".toast-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; }\n";

    @Override
    public void start(Stage stage) {
        setAppIcon(stage);
        LicenseService.Status st = LicenseService.checkStatus();

        if (st == LicenseService.Status.CLOCK_TAMPERED) {
            new Alert(Alert.AlertType.ERROR, "⚠️ تم اكتشاف تلاعب في ساعة وتاريخ النظام! تم إيقاف التشغيل لحماية البيانات.").showAndWait();
            Platform.exit();
            return;
        }

        if (st != LicenseService.Status.VALID) {
            showActivationScreen(stage, st == LicenseService.Status.EXPIRED ? "⚠️ لقد انتهت فترة صلاحية ترخيصك! يرجى التجديد:" : null);
        } else {
            showLogin(stage);
        }
    }

    private InputStream getLogoStream() {
        InputStream is = getClass().getResourceAsStream("/assets/logo.png");
        if (is == null) is = getClass().getResourceAsStream("/assets/logo40dash.jpeg");
        return is;
    }

    private void setAppIcon(Stage stage) {
        try (InputStream is = getLogoStream()) {
            if (is != null) stage.getIcons().add(new Image(is));
        } catch (Exception ignored) {}
    }

    private ImageView getLogoView(double width) {
        try (InputStream is = getLogoStream()) {
            if (is != null) {
                Image img = new Image(is);
                ImageView iv = new ImageView(img);
                iv.setFitWidth(width);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                return iv;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private HBox createBrandingFooter() {
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER);
        footer.setStyle("-fx-background-color: #080f1a; -fx-padding: 10 20;");

        Label copy = new Label("جميع الحقوق محفوظة © 2026 | تطوير: " + DEV_NAME + " | هاتف: " + DEV_PHONE + " | ");
        copy.setTextFill(Color.web("#94a3b8"));
        copy.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Hyperlink linkedinLink = new Hyperlink("LinkedIn Profile");
        linkedinLink.setStyle("-fx-text-fill: #38bdf8; -fx-underline: true; -fx-font-weight: bold;");
        linkedinLink.setOnAction(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(DEV_LINKEDIN));
                }
            } catch (Exception ignored) {}
        });

        footer.getChildren().addAll(copy, linkedinLink);
        return footer;
    }

    private void showActivationScreen(Stage stage, String warning) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0c1626;");

        VBox card = new VBox(15);
        card.setMaxWidth(480);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);

        ImageView logo = getLogoView(200);
        Label title = new Label("🔒 تفعيل وتجديد ترخيص المنظومة");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        Label note = new Label(warning != null ? warning : "أهلاً بك! يرجى إرسال كود البصمة التالي للمطور لتفعيل نسختك:");
        note.setStyle(warning != null ? "-fx-text-fill: #ef4444; -fx-font-weight: bold;" : "");
        note.setWrapText(true);

        String hwid = LicenseService.getHWID();
        TextField hwidField = new TextField(hwid);
        hwidField.setEditable(false);
        hwidField.setStyle("-fx-background-color: #f1f5f9; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button copyBtn = new Button("📋 نسخ كود البصمة");
        copyBtn.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(hwid);
            clipboard.setContent(content);
            copyBtn.setText("✅ تم نسخ الكود بنجاح!");
        });

        TextField keyField = new TextField();
        keyField.setPromptText("أدخل مفتاح التفعيل (40D-XXXX-YYYYMMDD-ZZZZ)");
        keyField.setPrefHeight(42);

        Button actBtn = new Button("تأكيد التفعيل وتشغيل النظام");
        actBtn.getStyleClass().add("button-success");
        actBtn.setPrefSize(Double.MAX_VALUE, 45);

        actBtn.setOnAction(e -> {
            if (LicenseService.activate(keyField.getText())) {
                new Alert(Alert.AlertType.INFORMATION, "🎉 تم تفعيل المنظومة بنجاح!\n" + LicenseService.getLicenseInfo()).showAndWait();
                showLogin(stage);
            } else {
                new Alert(Alert.AlertType.ERROR, "❌ كود التفعيل غير صالح أو منتهي الصلاحية لهذا الجهاز!").show();
            }
        });

        if (logo != null) card.getChildren().add(logo);
        card.getChildren().addAll(title, note, hwidField, copyBtn, new Separator(), keyField, actBtn);
        root.getChildren().addAll(card, createBrandingFooter());

        Scene s = new Scene(root, 750, 600);
        s.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", ""));
        stage.setScene(s);
        stage.setTitle(SYSTEM_NAME + " - تفعيل الترخيص");
        stage.centerOnScreen();
        stage.show();
    }

    private void showToast(String msg, boolean isError) {
        Platform.runLater(() -> {
            Label l = new Label(msg);
            l.getStyleClass().add(isError ? "toast" : "toast-success");
            notificationArea.getChildren().add(l);
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> notificationArea.getChildren().remove(l));
                } catch (Exception ignored) {}
            }).start();
        });
    }

    private void showLogin(Stage stage) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #0c1626;");

        VBox card = new VBox(15);
        card.setMaxWidth(420);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);

        ImageView logo = getLogoView(220);
        Label title = new Label("تسجيل الدخول للموظفين");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));

        TextField u = new TextField(); u.setPromptText("اسم المستخدم"); u.setPrefHeight(42);
        PasswordField p = new PasswordField(); p.setPromptText("كلمة المرور"); p.setPrefHeight(42);
        Button btn = new Button("دخول المنظومة"); btn.getStyleClass().add("button-primary"); btn.setPrefSize(Double.MAX_VALUE, 45);

        btn.setOnAction(e -> {
            try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username=?")) {
                ps.setString(1, u.getText());
                ResultSet rs = ps.executeQuery();
                if (rs.next() && PasswordHasher.verify(p.getText(), rs.getString("password"))) {
                    loggedUser = rs.getString("username");
                    loggedRole = rs.getString("role");
                    showMain(stage);
                } else { showToast("❌ بيانات الدخول غير صحيحة", true); }
            } catch (Exception ex) { showToast("خطأ: " + ex.getMessage(), true); }
        });

        if (logo != null) card.getChildren().add(logo);
        card.getChildren().addAll(title, u, p, btn);
        root.getChildren().addAll(card, createBrandingFooter());

        Scene s = new Scene(root, 1000, 720);
        s.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", ""));
        stage.setScene(s);
        stage.setTitle(SYSTEM_NAME + " - تسجيل الدخول");
        stage.centerOnScreen();
        stage.show();
    }

    private void showMain(Stage stage) {
        StackPane mainStack = new StackPane();
        BorderPane root = new BorderPane();
        TabPane tabs = new TabPane();
        notificationArea.setAlignment(Pos.BOTTOM_RIGHT);
        notificationArea.setPadding(new Insets(30));
        notificationArea.setPickOnBounds(false);

        DashboardPane dashPane = new DashboardPane();
        Tab cashierTab = new Tab("🛒 الكاشير", new CashierPane(stage));
        Tab inventoryTab = new Tab("📦 المخزن", new InventoryPane());
        Tab expenseTab = new Tab("💸 المصروفات", new ExpensePane());
        Tab dashTab = new Tab("📊 الداشبورد", dashPane);
        Tab usersTab = new Tab("👥 الإدارة", new UsersPane());

        dashTab.setOnSelectionChanged(e -> { if (dashTab.isSelected()) dashPane.refresh(); });

        if ("ADMIN".equalsIgnoreCase(loggedRole)) {
            tabs.getTabs().addAll(cashierTab, inventoryTab, expenseTab, dashTab, usersTab);
        } else {
            tabs.getTabs().add(cashierTab);
        }

        HBox top = new HBox(20);
        top.getStyleClass().add("nav-bar");
        top.setAlignment(Pos.CENTER_RIGHT);

        ImageView topLogo = getLogoView(90);
        Label shop = new Label(SYSTEM_NAME + " | " + DatabaseManager.getProperty("branch.id", "MAIN"));
        shop.setTextFill(Color.WHITE);
        shop.setFont(Font.font("Segoe UI", FontWeight.BOLD, 17));

        Label licenseStatus = new Label("🛡️ " + LicenseService.getLicenseInfo());
        licenseStatus.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #38bdf8; -fx-padding: 5 12; -fx-background-radius: 6;");

        Button logout = new Button("تسجيل الخروج");
        logout.getStyleClass().add("button-danger");
        logout.setOnAction(e -> showLogin(stage));

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label curU = new Label("المستخدم: " + loggedUser + " (" + loggedRole + ")");
        curU.setTextFill(Color.web("#94a3b8"));

        if (topLogo != null) top.getChildren().add(topLogo);
        top.getChildren().addAll(shop, sp, licenseStatus, curU, logout);

        root.setTop(top);
        root.setCenter(tabs);
        root.setBottom(createBrandingFooter());
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        mainStack.getChildren().addAll(root, notificationArea);
        Scene s = new Scene(mainStack, 1366, 768);
        s.getStylesheets().add("data:text/css," + CSS.replaceAll("\n", ""));
        stage.setScene(s);
        stage.setMaximized(true);
        stage.setTitle(SYSTEM_NAME);
        stage.show();
    }

    class CashierPane extends BorderPane {
        ObservableList<OrderItem> cart = FXCollections.observableArrayList();
        Label totalLbl = new Label("0.00");
        Label changeLbl = new Label("0.00");
        TextField search = new TextField();
        TextField paidField = new TextField();

        CashierPane(Stage stage) {
            setPadding(new Insets(20));
            search.setPromptText("الكمية * الباركود...");
            search.setPrefHeight(50);
            search.setStyle("-fx-font-size: 18px;");

            search.setOnAction(e -> {
                String in = search.getText().trim();
                if (in.isEmpty()) return;
                int reqQty = 1;
                String barcode = in;
                if (in.contains("*")) {
                    try {
                        String[] pts = in.split("\\*");
                        reqQty = Integer.parseInt(pts[0].trim());
                        barcode = pts[1].trim();
                    } catch (Exception ignored) { return; }
                }
                String finalB = barcode;
                int finalQty = reqQty;
                try {
                    productRepo.findByBarcode(finalB).ifPresentOrElse(p -> {
                        int cur = cart.stream().filter(i -> i.getBarcode().equals(finalB)).mapToInt(OrderItem::getQty).sum();
                        if (cur + finalQty > p.getQuantity()) {
                            showToast("❌ الكمية غير كافية! المتاح: " + p.getQuantity(), true);
                        } else {
                            cart.add(new OrderItem(p.getBarcode(), p.getName(), p.getPrice(), p.getCost(), finalQty));
                            updateTotal();
                            search.clear();
                        }
                    }, () -> showToast("⚠️ المنتج غير مسجل بالمخزن!", true));
                } catch (Exception ex) { showToast(ex.getMessage(), true); }
            });

            TableView<OrderItem> table = new TableView<>(cart);
            table.setEditable(true);
            TableColumn<OrderItem, String> c1 = new TableColumn<>("الصنف"); c1.setCellValueFactory(new PropertyValueFactory<>("name"));
            TableColumn<OrderItem, Integer> c2 = new TableColumn<>("الكمية"); c2.setCellValueFactory(new PropertyValueFactory<>("qty"));
            c2.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
            c2.setOnEditCommit(t -> { t.getRowValue().setQty(t.getNewValue()); updateTotal(); });
            TableColumn<OrderItem, BigDecimal> c3 = new TableColumn<>("الإجمالي"); c3.setCellValueFactory(new PropertyValueFactory<>("total"));
            table.getColumns().addAll(c1, c2, c3);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            VBox side = new VBox(15); side.setPadding(new Insets(0, 0, 0, 20)); side.setPrefWidth(350); side.getStyleClass().add("card");
            totalLbl.getStyleClass().add("total-label");
            paidField.setPrefHeight(45); paidField.setStyle("-fx-font-size: 20px;");
            paidField.textProperty().addListener((o, old, nw) -> {
                try {
                    BigDecimal p = new BigDecimal(nw);
                    BigDecimal t = new BigDecimal(totalLbl.getText());
                    changeLbl.setText(p.subtract(t).toString());
                } catch (Exception ex) { changeLbl.setText("0.00"); }
            });

            Button pay = new Button("إتمام ومعاينة الفاتورة"); pay.getStyleClass().add("button-success"); pay.setPrefSize(Double.MAX_VALUE, 55);
            pay.setOnAction(ex -> { if (!cart.isEmpty()) showPreview(); else showToast("⚠️ السلة فارغة!", true); });

            Button clear = new Button("❌ تفريغ السلة"); clear.getStyleClass().add("button-danger"); clear.setMaxWidth(Double.MAX_VALUE);
            clear.setOnAction(ex -> { cart.clear(); updateTotal(); });

            Button refund = new Button("↩️ تسجيل مرتجع صنف"); refund.getStyleClass().add("button-primary"); refund.setMaxWidth(Double.MAX_VALUE);
            refund.setOnAction(ex -> showRefundDialog());

            Button shift = new Button("🔒 تقفيل الوردية الحالية"); shift.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
            shift.setPrefSize(Double.MAX_VALUE, 45); shift.setOnAction(ex -> showShiftDialog(stage));

            side.getChildren().addAll(new Label("الإجمالي المستحق:"), totalLbl, new Label("المدفوع:"), paidField, new Label("الباقي:"), changeLbl, pay, clear, refund, shift);
            setTop(search); setCenter(table); setRight(side);
        }

        private void updateTotal() {
            BigDecimal t = cart.stream().map(OrderItem::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalLbl.setText(t.toString());
        }

        private void showPreview() {
            Stage st = new Stage(); st.initModality(Modality.APPLICATION_MODAL);
            VBox v = new VBox(10); v.setPadding(new Insets(20)); v.setAlignment(Pos.TOP_CENTER);
            
            ImageView rLogo = getLogoView(120);
            if (rLogo != null) v.getChildren().add(rLogo);

            v.getChildren().addAll(
                new Label("--- " + SYSTEM_NAME + " ---"),
                new Label("Cashier: " + loggedUser + " | Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
                new Separator()
            );
            
            for (OrderItem i : cart) v.getChildren().add(new Label(i.getName() + " x" + i.getQty() + " = " + i.getTotal()));
            
            v.getChildren().addAll(
                new Separator(),
                new Label("TOTAL: " + totalLbl.getText() + " EGP"),
                new Label("PAID: " + paidField.getText()),
                new Label("CHANGE: " + changeLbl.getText()),
                new Separator(),
                new Label("تطوير: " + DEV_NAME),
                new Label("الدعم: " + DEV_PHONE)
            );

            Button printBtn = new Button("طباعة وحفظ الفاتورة"); printBtn.getStyleClass().add("button-success");
            printBtn.setOnAction(e -> {
                try {
                    printReceipt();
                    int id = salesService.checkout(cart, loggedUser);
                    cart.clear(); updateTotal(); paidField.clear(); changeLbl.setText("0.00");
                    showToast("✅ تم حفظ الفاتورة بنجاح رقم: " + id, false);
                    st.close();
                } catch (Exception ex) { showToast("❌ فشل الحفظ: " + ex.getMessage(), true); }
            });
            v.getChildren().add(printBtn);
            st.setScene(new Scene(v, 370, 560)); st.setTitle("معاينة الفاتورة"); st.show();
        }

        private void printReceipt() {
            PrinterJob job = PrinterJob.getPrinterJob();
            job.setPrintable((graphics, pf, pi) -> {
                if (pi > 0) return 1;
                Graphics2D g = (Graphics2D) graphics;
                g.translate(pf.getImageableX(), pf.getImageableY());
                int y = 15;
                g.drawString(SYSTEM_NAME, 10, y); y += 15;
                g.drawString("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 10, y); y += 15;
                g.drawString("Cashier: " + loggedUser, 10, y); y += 15;
                g.drawString("--------------------------------", 10, y); y += 15;
                for (OrderItem i : cart) {
                    g.drawString(i.getName() + " x" + i.getQty() + " = " + i.getTotal(), 10, y);
                    y += 15;
                }
                g.drawString("--------------------------------", 10, y); y += 15;
                g.drawString("TOTAL: " + totalLbl.getText() + " EGP", 10, y);
                g.drawString("--------------------------------", 10, y); y += 15;
                g.drawString("Powered by " + DEV_NAME, 10, y); y += 15;
                g.drawString("Support: " + DEV_PHONE, 10, y);
                return 0;
            });
            try { job.print(); } catch (Exception ignored) {}
        }

        private void showRefundDialog() {
            Stage st = new Stage(); st.initModality(Modality.APPLICATION_MODAL);
            GridPane g = new GridPane(); g.setPadding(new Insets(20)); g.setHgap(10); g.setVgap(10);
            TextField bF = new TextField(); TextField qF = new TextField("1");
            Button sub = new Button("تأكيد المرتجع"); sub.getStyleClass().add("button-danger");
            sub.setOnAction(e -> {
                try {
                    salesService.refund(bF.getText(), Integer.parseInt(qF.getText()), loggedUser);
                    showToast("↩️ تم قبول المرتجع للمخزن والدرج", false);
                    st.close();
                } catch (Exception ex) { showToast(ex.getMessage(), true); }
            });
            g.add(new Label("الباركود:"), 0, 0); g.add(bF, 1, 0);
            g.add(new Label("الكمية:"), 0, 1); g.add(qF, 1, 1);
            g.add(sub, 1, 2);
            st.setScene(new Scene(g, 380, 180)); st.setTitle("تسجيل مرتجع صنف"); st.show();
        }

        private void showShiftDialog(Stage mainStage) {
            try {
                BigDecimal[] stats = appRepo.getShiftStats(loggedUser, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                Stage st = new Stage(); st.initModality(Modality.APPLICATION_MODAL);
                VBox v = new VBox(15); v.setPadding(new Insets(25)); v.setAlignment(Pos.TOP_CENTER);
                v.getChildren().addAll(
                    new Label("📊 جرد الشيفت للموظف: " + loggedUser), new Separator(),
                    new Label("إجمالي المبيعات: " + stats[0] + " EGP"),
                    new Label("إجمالي المصروفات: " + stats[1] + " EGP"),
                    new Label("💵 الصافي في الدرج: " + stats[2] + " EGP")
                );
                Button out = new Button("🔒 إغلاق وتسجيل الخروج"); out.getStyleClass().add("button-danger");
                out.setOnAction(e -> { st.close(); showLogin(mainStage); });
                v.getChildren().add(out);
                st.setScene(new Scene(v, 400, 320)); st.setTitle("تقرير الوردية"); st.show();
            } catch (Exception ex) { showToast(ex.getMessage(), true); }
        }
    }

    class InventoryPane extends BorderPane {
        ObservableList<Product> list = FXCollections.observableArrayList();
        TableView<Product> table = new TableView<>(list);

        InventoryPane() {
            setPadding(new Insets(20));
            HBox top = new HBox(15); top.setPadding(new Insets(0, 0, 15, 0));
            TextField b = new TextField(); b.setPromptText("باركود");
            TextField n = new TextField(); n.setPromptText("اسم الصنف");
            TextField p = new TextField(); p.setPromptText("سعر البيع");
            TextField c = new TextField(); c.setPromptText("التكلفة");
            TextField q = new TextField(); q.setPromptText("الكمية");
            Button save = new Button("حفظ المنتج"); save.getStyleClass().add("button-success");

            save.setOnAction(e -> {
                try {
                    productRepo.save(new Product(b.getText(), n.getText(), new BigDecimal(p.getText()), new BigDecimal(c.getText()), Integer.parseInt(q.getText())));
                    refresh();
                    b.clear(); n.clear(); p.clear(); c.clear(); q.clear();
                    showToast("✅ تم حفظ وتحديث المنتج", false);
                } catch (Exception ex) { showToast("خطأ في البيانات: " + ex.getMessage(), true); }
            });

            top.getChildren().addAll(b, n, p, c, q, save);
            TableColumn<Product, String> c1 = new TableColumn<>("باركود"); c1.setCellValueFactory(new PropertyValueFactory<>("barcode"));
            TableColumn<Product, String> c2 = new TableColumn<>("اسم الصنف"); c2.setCellValueFactory(new PropertyValueFactory<>("name"));
            TableColumn<Product, BigDecimal> c3 = new TableColumn<>("سعر البيع"); c3.setCellValueFactory(new PropertyValueFactory<>("price"));
            TableColumn<Product, Integer> c4 = new TableColumn<>("الكمية"); c4.setCellValueFactory(new PropertyValueFactory<>("quantity"));
            table.getColumns().addAll(c1, c2, c3, c4);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            setTop(top); setCenter(table);
            refresh();
        }
        void refresh() { try { list.setAll(productRepo.findAll()); } catch (Exception ignored) {} }
    }

    class ExpensePane extends BorderPane {
        ObservableList<Expense> list = FXCollections.observableArrayList();
        TableView<Expense> table = new TableView<>(list);

        ExpensePane() {
            setPadding(new Insets(20));
            HBox top = new HBox(15); top.setPadding(new Insets(0, 0, 15, 0));
            TextField t = new TextField(); t.setPromptText("بند المصروف");
            TextField a = new TextField(); a.setPromptText("المبلغ");
            Button save = new Button("تسجيل المصروف"); save.getStyleClass().add("button-danger");

            save.setOnAction(e -> {
                try {
                    appRepo.saveExpense(new Expense(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), t.getText(), new BigDecimal(a.getText()), loggedUser));
                    refresh(); t.clear(); a.clear();
                    showToast("💸 تم تسجيل المصروف", false);
                } catch (Exception ex) { showToast("خطأ: " + ex.getMessage(), true); }
            });

            top.getChildren().addAll(t, a, save);
            TableColumn<Expense, String> c1 = new TableColumn<>("التاريخ"); c1.setCellValueFactory(new PropertyValueFactory<>("date"));
            TableColumn<Expense, String> c2 = new TableColumn<>("البند"); c2.setCellValueFactory(new PropertyValueFactory<>("title"));
            TableColumn<Expense, BigDecimal> c3 = new TableColumn<>("المبلغ"); c3.setCellValueFactory(new PropertyValueFactory<>("amount"));
            TableColumn<Expense, String> c4 = new TableColumn<>("الموظف"); c4.setCellValueFactory(new PropertyValueFactory<>("user"));
            table.getColumns().addAll(c1, c2, c3, c4);
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            setTop(top); setCenter(table);
            refresh();
        }
        void refresh() { try { list.setAll(appRepo.findAllExpenses()); } catch (Exception ignored) {} }
    }

    class DashboardPane extends VBox {
        Label sL = new Label("0.00");
        Label eL = new Label("0.00");
        Label pL = new Label("0.00");

        DashboardPane() {
            setPadding(new Insets(40)); setSpacing(30); setAlignment(Pos.TOP_CENTER);
            HBox h = new HBox(30, card("إجمالي المبيعات", sL), card("إجمالي المصاريف", eL), card("صافي الأرباح", pL));
            h.setAlignment(Pos.CENTER);
            getChildren().addAll(new Label("📈 تحليلات الأداء المالي") {{ setFont(Font.font("System", FontWeight.BOLD, 24)); }}, h);
            refresh();
        }
        void refresh() {
            try {
                BigDecimal[] st = appRepo.getFinancialStats();
                sL.setText(st[0] + " EGP"); eL.setText(st[1] + " EGP"); pL.setText(st[2] + " EGP");
            } catch (Exception ignored) {}
        }
        VBox card(String title, Label val) {
            VBox v = new VBox(15, new Label(title) {{ setFont(Font.font(18)); }}, val);
            v.getStyleClass().add("card"); val.setStyle("-fx-font-size: 30px; -fx-font-weight: bold;");
            v.setMinWidth(280); v.setAlignment(Pos.CENTER);
            return v;
        }
    }

    class UsersPane extends VBox {
        ObservableList<UserData> list = FXCollections.observableArrayList();
        TableView<UserData> table = new TableView<>(list);

        UsersPane() {
            setPadding(new Insets(20)); setSpacing(15);
            HBox top = new HBox(15);
            TextField u = new TextField(); u.setPromptText("اسم المستخدم");
            PasswordField p = new PasswordField(); p.setPromptText("كلمة المرور");
            ComboBox<String> r = new ComboBox<>(FXCollections.observableArrayList("ADMIN", "CASHIER")); r.setValue("CASHIER");
            Button save = new Button("حفظ الموظف"); save.getStyleClass().add("button-success");

            save.setOnAction(e -> {
                try {
                    appRepo.saveUser(u.getText(), PasswordHasher.hash(p.getText()), r.getValue());
                    refresh(); u.clear(); p.clear();
                    showToast("✅ تم حفظ المستخدم مشفراً", false);
                } catch (Exception ex) { showToast(ex.getMessage(), true); }
            });

            top.getChildren().addAll(u, p, r, save);
            TableColumn<UserData, String> c1 = new TableColumn<>("المستخدم"); c1.setCellValueFactory(new PropertyValueFactory<>("username"));
            TableColumn<UserData, String> c2 = new TableColumn<>("الصلاحية"); c2.setCellValueFactory(new PropertyValueFactory<>("role"));
            table.getColumns().addAll(c1, c2); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            getChildren().addAll(new Label("إدارة صلاحيات الموظفين"), top, table);
            refresh();
        }
        void refresh() { try { list.setAll(appRepo.findAllUsers()); } catch (Exception ignored) {} }
    }

    public static void main(String[] args) { launch(args); }
}