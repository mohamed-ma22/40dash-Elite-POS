package com.fortydash;

import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

public class App extends Application {
   private static final String DB_URL = "jdbc:sqlite:40dash.db";
   private static String loggedUser = "";
   private static String loggedRole = "";
   private static VBox notificationArea = new VBox((double)5.0F);
   private static final String LOGO_URL = "https://i.imgur.com/8L8O5R1.png";
   private static final String CSS = ".root { -fx-font-family: 'Segoe UI Semibold'; -fx-base: #f1f5f9; }\n.nav-bar { -fx-background-color: #0f172a; -fx-padding: 10 20; }\n.tab-pane .tab-header-area .tab-header-background { -fx-background-color: #1e293b; }\n.tab { -fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 25; }\n.tab:selected { -fx-background-color: #10b981; }\n.button-primary { -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.button-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.button-danger { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.card { -fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 8); }\n.total-label { -fx-text-fill: #1e293b; -fx-font-size: 50px; -fx-font-weight: 900; }\n.toast { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; -fx-font-size: 14px; }\n.toast-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; }\n";

   private static String hashPassword(String base) {
      try {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         byte[] hash = digest.digest(base.getBytes("UTF-8"));
         StringBuilder hexString = new StringBuilder();

         for(byte b : hash) {
            String hex = Integer.toHexString(255 & b);
            if (hex.length() == 1) {
               hexString.append('0');
            }

            hexString.append(hex);
         }

         return hexString.toString();
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public void start(Stage stage) {
      this.initDB();
      this.showLogin(stage);
   }

   private void initDB() {
      try {
         Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

         try {
            Statement s = c.createStatement();

            try {
               s.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT, role TEXT)");
               s.execute("INSERT OR IGNORE INTO users VALUES ('admin', '" + hashPassword("admin") + "', 'ADMIN')");
               s.execute("CREATE TABLE IF NOT EXISTS products (barcode TEXT PRIMARY KEY, name TEXT, price REAL, cost REAL, quantity INTEGER)");
               s.execute("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, total REAL, profit REAL, cashier TEXT)");
               s.execute("CREATE TABLE IF NOT EXISTS order_items (id INTEGER PRIMARY KEY AUTOINCREMENT, order_id INTEGER, barcode TEXT, name TEXT, qty INTEGER, total REAL, FOREIGN KEY(order_id) REFERENCES orders(id))");
               s.execute("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, title TEXT, amount REAL, user TEXT)");
            } catch (Throwable var7) {
               if (s != null) {
                  try {
                     s.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (s != null) {
               s.close();
            }
         } catch (Throwable var8) {
            if (c != null) {
               try {
                  c.close();
               } catch (Throwable var5) {
                  var8.addSuppressed(var5);
               }
            }

            throw var8;
         }

         if (c != null) {
            c.close();
         }
      } catch (Exception var9) {
      }

   }

   private void showToast(String msg, boolean isError) {
      Platform.runLater(() -> {
         Label l = new Label(msg);
         l.getStyleClass().add(isError ? "toast" : "toast-success");
         notificationArea.getChildren().add(l);
         (new Thread(() -> {
            try {
               Thread.sleep(3000L);
               Platform.runLater(() -> notificationArea.getChildren().remove(l));
            } catch (Exception var2) {
            }

         })).start();
      });
   }

   private void showLogin(Stage stage) {
      VBox root = new VBox((double)25.0F);
      root.setAlignment(Pos.CENTER);
      root.setStyle("-fx-background-color: #0f172a;");
      VBox card = new VBox((double)20.0F);
      card.setMaxWidth((double)400.0F);
      card.getStyleClass().add("card");
      card.setAlignment(Pos.CENTER);

      try {
         ImageView loginLogo = new ImageView(new Image("https://i.imgur.com/8L8O5R1.png", true));
         loginLogo.setFitWidth((double)180.0F);
         loginLogo.setPreserveRatio(true);
         card.getChildren().add(loginLogo);
      } catch (Exception var8) {
      }

      TextField u = new TextField();
      u.setPromptText("اسم المستخدم");
      u.setPrefHeight((double)40.0F);
      PasswordField p = new PasswordField();
      p.setPromptText("كلمة المرور");
      p.setPrefHeight((double)40.0F);
      Button btn = new Button("دخول النظام");
      btn.getStyleClass().add("button-primary");
      btn.setPrefSize(Double.MAX_VALUE, (double)45.0F);
      btn.setOnAction((e) -> {
         try {
            Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

            try {
               PreparedStatement ps = c.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");

               try {
                  ps.setString(1, u.getText());
                  ps.setString(2, hashPassword(p.getText()));
                  ResultSet rs = ps.executeQuery();
                  if (rs.next()) {
                     loggedUser = rs.getString("username");
                     loggedRole = rs.getString("role");
                     this.showMain(stage);
                  } else {
                     this.showToast("❌ بيانات الدخول غير صحيحة", true);
                  }
               } catch (Throwable var11) {
                  if (ps != null) {
                     try {
                        ps.close();
                     } catch (Throwable var10) {
                        var11.addSuppressed(var10);
                     }
                  }

                  throw var11;
               }

               if (ps != null) {
                  ps.close();
               }
            } catch (Throwable var12) {
               if (c != null) {
                  try {
                     c.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (c != null) {
               c.close();
            }
         } catch (Exception var13) {
         }

      });
      card.getChildren().addAll(new Label("تسجيل الدخول للموظفين") {
         {
            this.setFont(Font.font((double)16.0F));
         }
      }, u, p, btn);
      root.getChildren().add(card);
      Scene s = new Scene(root, (double)1000.0F, (double)700.0F);
      s.getStylesheets().add("data:text/css," + ".root { -fx-font-family: 'Segoe UI Semibold'; -fx-base: #f1f5f9; }\n.nav-bar { -fx-background-color: #0f172a; -fx-padding: 10 20; }\n.tab-pane .tab-header-area .tab-header-background { -fx-background-color: #1e293b; }\n.tab { -fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 25; }\n.tab:selected { -fx-background-color: #10b981; }\n.button-primary { -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.button-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.button-danger { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.card { -fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 8); }\n.total-label { -fx-text-fill: #1e293b; -fx-font-size: 50px; -fx-font-weight: 900; }\n.toast { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; -fx-font-size: 14px; }\n.toast-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; }\n".replaceAll("\n", ""));
      stage.setScene(s);
      stage.centerOnScreen();
      stage.show();
   }

   private void showMain(Stage stage) {
      StackPane mainStack = new StackPane();
      BorderPane root = new BorderPane();
      TabPane tabs = new TabPane();
      notificationArea.setAlignment(Pos.BOTTOM_RIGHT);
      notificationArea.setPadding(new Insets((double)30.0F));
      notificationArea.setPickOnBounds(false);
      DashboardPane dashPane = new DashboardPane();
      Tab cashierTab = new Tab("\ud83d\uded2 الكاشير", new CashierPane(stage));
      Tab inventoryTab = new Tab("\ud83d\udce6 المخزن", new InventoryPane());
      Tab expenseTab = new Tab("\ud83d\udcb8 المصروفات", new ExpensePane());
      Tab dashTab = new Tab("\ud83d\udcca الداشبورد", dashPane);
      Tab usersTab = new Tab("\ud83d\udc65 الإدارة", new UsersPane());
      dashTab.setOnSelectionChanged((e) -> {
         if (dashTab.isSelected()) {
            dashPane.refreshStats();
         }

      });
      if (loggedRole.equalsIgnoreCase("ADMIN")) {
         tabs.getTabs().addAll(cashierTab, inventoryTab, expenseTab, dashTab, usersTab);
      } else {
         tabs.getTabs().add(cashierTab);
      }

      HBox top = new HBox((double)20.0F);
      top.getStyleClass().add("nav-bar");
      top.setAlignment(Pos.CENTER_RIGHT);
      Label shopName = new Label("40DASH ELITE POS") {
         {
            this.setTextFill(Color.WHITE);
            this.setFont(Font.font("System", FontWeight.BOLD, (double)18.0F));
         }
      };
      Button logoutBtn = new Button("تسجيل الخروج");
      logoutBtn.getStyleClass().add("button-danger");
      logoutBtn.setOnAction((e) -> this.showLogin(stage));
      Region spacer = new Region();
      HBox.setHgrow(spacer, Priority.ALWAYS);
      top.getChildren().addAll(shopName, spacer, new Label("الموظف الحالي: " + loggedUser) {
         {
            this.setTextFill(Color.WHITE);
         }
      }, logoutBtn);
      root.setTop(top);
      root.setCenter(tabs);
      root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
      mainStack.getChildren().addAll(root, notificationArea);
      Scene s = new Scene(mainStack, (double)1366.0F, (double)768.0F);
      s.getStylesheets().add("data:text/css," + ".root { -fx-font-family: 'Segoe UI Semibold'; -fx-base: #f1f5f9; }\n.nav-bar { -fx-background-color: #0f172a; -fx-padding: 10 20; }\n.tab-pane .tab-header-area .tab-header-background { -fx-background-color: #1e293b; }\n.tab { -fx-background-color: #334155; -fx-text-fill: white; -fx-padding: 12 25; }\n.tab:selected { -fx-background-color: #10b981; }\n.button-primary { -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.button-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.button-danger { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; }\n.card { -fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 8); }\n.total-label { -fx-text-fill: #1e293b; -fx-font-size: 50px; -fx-font-weight: 900; }\n.toast { -fx-background-color: #ef4444; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; -fx-font-size: 14px; }\n.toast-success { -fx-background-color: #10b981; -fx-text-fill: white; -fx-padding: 12 25; -fx-background-radius: 25; }\n".replaceAll("\n", ""));
      stage.setScene(s);
      stage.setMaximized(true);
      stage.show();
   }

   public static void main(String[] args) {
      launch(args);
   }

   class CashierPane extends BorderPane {
      ObservableList<SaleItem> cart = FXCollections.<SaleItem>observableArrayList();
      Label totalLbl = new Label("0.00");
      Label changeLbl = new Label("0.00");
      TextField search = new TextField();
      TextField paidField = new TextField();

      CashierPane(Stage stage) {
         this.setPadding(new Insets((double)20.0F));
         this.search.setPromptText("الكمية * الباركود...");
         this.search.setPrefHeight((double)50.0F);
         this.search.setStyle("-fx-font-size: 18px;");
         this.search.setOnAction((e) -> {
            String input = this.search.getText();
            if (!input.isEmpty()) {
               int reqQty = 1;
               String b = input;
               if (input.contains("*")) {
                  try {
                     String[] pts = input.split("\\*");
                     reqQty = Integer.parseInt(pts[0]);
                     b = pts[1];
                  } catch (Exception var13) {
                     return;
                  }
               }

               String finalB = b;

               try {
                  Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

                  try {
                     PreparedStatement ps = c.prepareStatement("SELECT * FROM products WHERE barcode=?");

                     try {
                        ps.setString(1, finalB);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                           int stock = rs.getInt("quantity");
                           int currentInCart = this.cart.stream().filter((i) -> i.getBarcode().equals(finalB)).mapToInt(SaleItem::getQty).sum();
                           if (currentInCart + reqQty > stock) {
                              App.this.showToast("❌ الكمية غير كافية! (المتاح: " + stock + ")", true);
                              Toolkit.getDefaultToolkit().beep();
                           } else {
                              this.cart.add(new SaleItem(rs.getString("barcode"), rs.getString("name"), rs.getDouble("price"), rs.getDouble("cost"), reqQty));
                              this.updateTotal();
                              this.search.clear();
                           }
                        } else {
                           App.this.showToast("⚠️ المنتج غير موجود!", true);
                        }
                     } catch (Throwable var14) {
                        if (ps != null) {
                           try {
                              ps.close();
                           } catch (Throwable var12) {
                              var14.addSuppressed(var12);
                           }
                        }

                        throw var14;
                     }

                     if (ps != null) {
                        ps.close();
                     }
                  } catch (Throwable var15) {
                     if (c != null) {
                        try {
                           c.close();
                        } catch (Throwable var11) {
                           var15.addSuppressed(var11);
                        }
                     }

                     throw var15;
                  }

                  if (c != null) {
                     c.close();
                  }
               } catch (Exception var16) {
               }

            }
         });
         TableView<SaleItem> table = new TableView<SaleItem>(this.cart);
         table.setEditable(true);
         TableColumn<SaleItem, String> c1 = new TableColumn<SaleItem, String>("الصنف");
         c1.setCellValueFactory(new PropertyValueFactory("name"));
         TableColumn<SaleItem, Integer> c2 = new TableColumn<SaleItem, Integer>("الكمية (عدل هنا)");
         c2.setCellValueFactory(new PropertyValueFactory("qty"));
         c2.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
         c2.setOnEditCommit((t) -> {
            try {
               Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

               try {
                  PreparedStatement ps = c.prepareStatement("SELECT quantity FROM products WHERE barcode=?");

                  try {
                     ps.setString(1, ((SaleItem)t.getRowValue()).getBarcode());
                     ResultSet rs = ps.executeQuery();
                     if (rs.next() && rs.getInt("quantity") >= (Integer)t.getNewValue()) {
                        ((SaleItem)t.getTableView().getItems().get(t.getTablePosition().getRow())).setQty((Integer)t.getNewValue());
                        this.updateTotal();
                     } else {
                        App.this.showToast("❌ الكمية المطلوبة أكبر من المخزن!", true);
                        t.getTableView().refresh();
                     }
                  } catch (Throwable var8) {
                     if (ps != null) {
                        try {
                           ps.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (ps != null) {
                     ps.close();
                  }
               } catch (Throwable var9) {
                  if (c != null) {
                     try {
                        c.close();
                     } catch (Throwable var6) {
                        var9.addSuppressed(var6);
                     }
                  }

                  throw var9;
               }

               if (c != null) {
                  c.close();
               }
            } catch (Exception var10) {
            }

         });
         TableColumn<SaleItem, Double> c3 = new TableColumn<SaleItem, Double>("الإجمالي");
         c3.setCellValueFactory(new PropertyValueFactory("total"));
         table.getColumns().addAll(c1, c2, c3);
         table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
         VBox side = new VBox((double)15.0F);
         side.setPadding(new Insets((double)0.0F, (double)0.0F, (double)0.0F, (double)20.0F));
         side.setPrefWidth((double)350.0F);
         side.getStyleClass().add("card");
         this.paidField.setPrefHeight((double)45.0F);
         this.paidField.setStyle("-fx-font-size: 20px;");
         this.paidField.textProperty().addListener((o, old, nw) -> {
            try {
               double p = Double.parseDouble(nw);
               double t = Double.parseDouble(this.totalLbl.getText());
               this.changeLbl.setText(String.format("%.2f", p - t));
            } catch (Exception var8) {
               this.changeLbl.setText("0.00");
            }

         });
         Button pay = new Button("إتمام ومعاينة الفاتورة");
         pay.getStyleClass().add("button-success");
         pay.setPrefSize(Double.MAX_VALUE, (double)60.0F);
         pay.setStyle("-fx-font-size: 16px;");
         pay.setOnAction((ex) -> {
            if (!this.cart.isEmpty()) {
               this.showReceiptPreview();
            } else {
               App.this.showToast("⚠️ الفاتورة فارغة!", true);
            }

         });
         Button clearBtn = new Button("❌ تفريغ السلة بالكامل");
         clearBtn.getStyleClass().add("button-danger");
         clearBtn.setPrefWidth(Double.MAX_VALUE);
         clearBtn.setOnAction((ex) -> {
            this.cart.clear();
            this.updateTotal();
            App.this.showToast("\ud83d\uddd1️ تم مسح السلة", false);
         });
         Button refundBtn = new Button("↩️ تسجيل مرتجع صنف");
         refundBtn.getStyleClass().add("button-primary");
         refundBtn.setPrefWidth(Double.MAX_VALUE);
         refundBtn.setOnAction((ex) -> this.showRefundDialog());
         Button closeShiftBtn = new Button("\ud83d\udd12 تقفيل الوردية الحالية");
         closeShiftBtn.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;");
         closeShiftBtn.setPrefSize(Double.MAX_VALUE, (double)50.0F);
         closeShiftBtn.setOnAction((ex) -> this.showShiftReport(stage));
         side.getChildren().addAll(new Label("الإجمالي المستحق:"), this.totalLbl, new Label("المبلغ المدفوع:"), this.paidField, new Label("الباقي:"), this.changeLbl, pay, clearBtn, refundBtn, closeShiftBtn);
         this.setTop(this.search);
         this.setCenter(table);
         this.setRight(side);
      }

      private void updateTotal() {
         this.totalLbl.setText(String.format("%.2f", this.cart.stream().mapToDouble(SaleItem::getTotal).sum()));
      }

      private void showReceiptPreview() {
         Stage st = new Stage();
         st.initModality(Modality.APPLICATION_MODAL);
         VBox v = new VBox((double)10.0F);
         v.setPadding(new Insets((double)20.0F));
         v.setAlignment(Pos.TOP_CENTER);
         v.getChildren().add(new Label("--- 40DASH POS RECEIPT ---") {
            // $FF: synthetic field
            final App.CashierPane this$1;

            {
               super(arg0);
               this.this$1 = this$1;
               this.setFont(Font.font("Courier New", FontWeight.BOLD, (double)18.0F));
            }
         });
         ObservableList var10000 = v.getChildren();
         LocalDateTime var10003 = LocalDateTime.now();
         var10000.add(new Label("Date: " + var10003.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
         v.getChildren().add(new Label("Cashier: " + App.loggedUser));
         v.getChildren().add(new Separator());

         for(SaleItem i : this.cart) {
            v.getChildren().add(new Label(String.format("%s x%d = %.2f", i.getName(), i.getQty(), i.getTotal())));
         }

         v.getChildren().add(new Separator());
         v.getChildren().add(new Label("TOTAL: " + this.totalLbl.getText()) {
            // $FF: synthetic field
            final App.CashierPane this$1;

            {
               super(arg0);
               this.this$1 = this$1;
               this.setFont(Font.font("System", FontWeight.BOLD, (double)16.0F));
            }
         });
         v.getChildren().add(new Label("PAID: " + this.paidField.getText()));
         v.getChildren().add(new Label("CHANGE: " + this.changeLbl.getText()));
         Button confirm = new Button("طباعة وحفظ الفاتورة");
         confirm.getStyleClass().add("button-success");
         confirm.setPrefWidth((double)200.0F);
         confirm.setOnAction((e) -> {
            this.printReceipt();
            this.saveSale();
            st.close();
         });
         v.getChildren().addAll(confirm);
         st.setScene(new Scene(v, (double)350.0F, (double)520.0F));
         st.setTitle("معاينة الفاتورة");
         st.show();
      }

      private void saveSale() {
         try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:40dash.db");

            try {
               conn.setAutoCommit(false);
               double totalProfit = this.cart.stream().mapToDouble((i) -> i.getTotal() - i.getCost() * (double)i.getQty()).sum();
               PreparedStatement pOrder = conn.prepareStatement("INSERT INTO orders (date, total, profit, cashier) VALUES (?,?,?,?)", 1);
               pOrder.setString(1, LocalDateTime.now().toString());
               pOrder.setDouble(2, Double.parseDouble(this.totalLbl.getText()));
               pOrder.setDouble(3, totalProfit);
               pOrder.setString(4, App.loggedUser);
               pOrder.executeUpdate();
               ResultSet rs = pOrder.getGeneratedKeys();
               int orderId = rs.next() ? rs.getInt(1) : 0;

               for(SaleItem itm : this.cart) {
                  PreparedStatement pItem = conn.prepareStatement("INSERT INTO order_items (order_id, barcode, name, qty, total) VALUES (?,?,?,?,?)");
                  pItem.setInt(1, orderId);
                  pItem.setString(2, itm.getBarcode());
                  pItem.setString(3, itm.getName());
                  pItem.setInt(4, itm.getQty());
                  pItem.setDouble(5, itm.getTotal());
                  pItem.executeUpdate();
                  PreparedStatement pStock = conn.prepareStatement("UPDATE products SET quantity = quantity - ? WHERE barcode = ?");
                  pStock.setInt(1, itm.getQty());
                  pStock.setString(2, itm.getBarcode());
                  pStock.executeUpdate();
               }

               conn.commit();
               this.cart.clear();
               this.totalLbl.setText("0.00");
               this.changeLbl.setText("0.00");
               this.paidField.clear();
               App.this.showToast("✅ تم حفظ الفاتورة رقم: " + orderId, false);
            } catch (Throwable var12) {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (Throwable var11) {
                     var12.addSuppressed(var11);
                  }
               }

               throw var12;
            }

            if (conn != null) {
               conn.close();
            }
         } catch (Exception var13) {
            App.this.showToast("❌ خطأ في الحفظ!", true);
         }

      }

      private void showRefundDialog() {
         Stage st = new Stage();
         st.initModality(Modality.APPLICATION_MODAL);
         st.setTitle("تسجيل صنف مرتجع للدرج والمخزن");
         GridPane g = new GridPane();
         g.setPadding(new Insets((double)20.0F));
         g.setHgap((double)10.0F);
         g.setVgap((double)10.0F);
         TextField barF = new TextField();
         barF.setPromptText("امسح باركود الصنف المرتجع");
         TextField qtyF = new TextField("1");
         Button submit = new Button("تأكيد إرجاع الصنف");
         submit.getStyleClass().add("button-danger");
         submit.setOnAction((e) -> {
            try {
               Connection conn = DriverManager.getConnection("jdbc:sqlite:40dash.db");

               try {
                  PreparedStatement ps = conn.prepareStatement("SELECT * FROM products WHERE barcode=?");
                  ps.setString(1, barF.getText());
                  ResultSet rs = ps.executeQuery();
                  if (rs.next()) {
                     int qty = Integer.parseInt(qtyF.getText());
                     double price = rs.getDouble("price");
                     double cost = rs.getDouble("cost");
                     conn.setAutoCommit(false);
                     PreparedStatement p1 = conn.prepareStatement("UPDATE products SET quantity = quantity + ? WHERE barcode = ?");
                     p1.setInt(1, qty);
                     p1.setString(2, barF.getText());
                     p1.executeUpdate();
                     PreparedStatement p2 = conn.prepareStatement("INSERT INTO orders (date, total, profit, cashier) VALUES (?,?,?,?)");
                     p2.setString(1, LocalDateTime.now().toString());
                     p2.setDouble(2, -(price * (double)qty));
                     p2.setDouble(3, -((price - cost) * (double)qty));
                     p2.setString(4, App.loggedUser);
                     p2.executeUpdate();
                     conn.commit();
                     App.this.showToast("↩️ تم قبول المرتجع وإعادته للمخزن وتعديل الحسابات", false);
                     st.close();
                  } else {
                     App.this.showToast("❌ باركود غير موجود بالمخزن!", true);
                  }
               } catch (Throwable var16) {
                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (Throwable var15) {
                        var16.addSuppressed(var15);
                     }
                  }

                  throw var16;
               }

               if (conn != null) {
                  conn.close();
               }
            } catch (Exception var17) {
               App.this.showToast("❌ خطأ في الإدخال", true);
            }

         });
         g.add(new Label("الباركود:"), 0, 0);
         g.add(barF, 1, 0);
         g.add(new Label("الكمية المرجعة:"), 0, 1);
         g.add(qtyF, 1, 1);
         g.add(submit, 1, 2);
         st.setScene(new Scene(g, (double)400.0F, (double)200.0F));
         st.show();
      }

      private void showShiftReport(Stage mainStage) {
         Stage st = new Stage();
         st.initModality(Modality.APPLICATION_MODAL);
         st.setTitle("تقرير إنهاء وردية الموظف");
         VBox v = new VBox((double)15.0F);
         v.setPadding(new Insets((double)25.0F));
         v.setAlignment(Pos.TOP_CENTER);
         double salesSum = (double)0.0F;
         double profitSum = (double)0.0F;
         double expSum = (double)0.0F;
         String todayPrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

         try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:40dash.db");

            try {
               PreparedStatement ps1 = conn.prepareStatement("SELECT SUM(total), SUM(profit) FROM orders WHERE cashier=? AND date LIKE ?");
               ps1.setString(1, App.loggedUser);
               ps1.setString(2, todayPrefix + "%");
               ResultSet rs1 = ps1.executeQuery();
               if (rs1.next()) {
                  salesSum = rs1.getDouble(1);
                  profitSum = rs1.getDouble(2);
               }

               PreparedStatement ps2 = conn.prepareStatement("SELECT SUM(amount) FROM expenses WHERE user=? AND date LIKE ?");
               ps2.setString(1, App.loggedUser);
               ps2.setString(2, todayPrefix + "%");
               ResultSet rs2 = ps2.executeQuery();
               if (rs2.next()) {
                  expSum = rs2.getDouble(1);
               }
            } catch (Throwable var17) {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (Throwable var16) {
                     var17.addSuppressed(var16);
                  }
               }

               throw var17;
            }

            if (conn != null) {
               conn.close();
            }
         } catch (Exception var18) {
         }

         double netCashInDrawer = salesSum - expSum;
         v.getChildren().add(new Label("\ud83d\udcca جرد الشيفت الحالي للموظف: " + App.loggedUser) {
            // $FF: synthetic field
            final App.CashierPane this$1;

            {
               super(arg0);
               this.this$1 = this$1;
               this.setFont(Font.font("System", FontWeight.BOLD, (double)16.0F));
            }
         });
         v.getChildren().add(new Separator());
         ObservableList var10000 = v.getChildren();
         Object[] var10004 = new Object[]{salesSum};
         var10000.add(new Label("إجمالي مبيعاتك اليوم: " + String.format("%.2f", var10004) + " EGP"));
         var10000 = v.getChildren();
         var10004 = new Object[]{expSum};
         var10000.add(new Label("إجمالي مصروفاتك اليوم: " + String.format("%.2f", var10004) + " EGP"));
         v.getChildren().add(new Label("\ud83d\udcb5 الصافي المفروض تفرزه في الدرج:") {
            // $FF: synthetic field
            final App.CashierPane this$1;

            {
               super(arg0);
               this.this$1 = this$1;
               this.setFont(Font.font((double)14.0F));
            }
         });
         var10004 = new Object[]{netCashInDrawer};
         Label cashLabel = new Label(String.format("%.2f", var10004) + " EGP") {
            // $FF: synthetic field
            final App.CashierPane this$1;

            {
               super(arg0);
               this.this$1 = this$1;
               this.setFont(Font.font("System", FontWeight.BOLD, (double)26.0F));
               this.setTextFill(Color.web("#10b981"));
            }
         };
         v.getChildren().add(cashLabel);
         Button logoutClose = new Button("\ud83d\udd12 طباعة الشيفت وتسجيل الخروج فوراً");
         logoutClose.getStyleClass().add("button-danger");
         logoutClose.setPrefHeight((double)45.0F);
         logoutClose.setOnAction((e) -> {
            App.this.showToast("\ud83d\udda8️ جاري طباعة تقرير الوردية للدرج...", false);
            st.close();
            App.this.showLogin(mainStage);
         });
         v.getChildren().add(logoutClose);
         st.setScene(new Scene(v, (double)450.0F, (double)400.0F));
         st.show();
      }

      private void printReceipt() {
         PrinterJob job = PrinterJob.getPrinterJob();
         job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) {
               return 1;
            } else {
               Graphics2D g2d = (Graphics2D)graphics;
               g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
               g2d.setFont(new java.awt.Font("Monospaced", 1, 10));
               int y = 15;
               g2d.drawString("40DASH POS SYSTEM", 10, y);
               y += 15;
               g2d.drawString("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 10, y);
               y += 15;
               g2d.drawString("Cashier: " + App.loggedUser, 10, y);
               y += 15;
               g2d.drawString("--------------------------------", 10, y);
               y += 15;

               for(SaleItem i : this.cart) {
                  g2d.drawString(i.getName(), 10, y);
                  y += 10;
                  int var10001 = i.getQty();
                  g2d.drawString(var10001 + " x " + i.getTotal() / (double)i.getQty() + " = " + i.getTotal(), 10, y);
                  y += 15;
               }

               g2d.drawString("--------------------------------", 10, y);
               y += 15;
               g2d.setFont(new java.awt.Font("Monospaced", 1, 12));
               g2d.drawString("TOTAL: " + this.totalLbl.getText() + " EGP", 10, y);
               y += 15;
               if (!this.paidField.getText().isEmpty()) {
                  g2d.drawString("PAID: " + this.paidField.getText(), 10, y);
                  y += 15;
                  g2d.drawString("CHANGE: " + this.changeLbl.getText(), 10, y);
               }

               return 0;
            }
         });

         try {
            job.print();
         } catch (PrinterException var3) {
         }

      }
   }

   class InventoryPane extends BorderPane {
      ObservableList<Product> masterData = FXCollections.<Product>observableArrayList();
      TableView<Product> table = new TableView<Product>();

      InventoryPane() {
         this.setPadding(new Insets((double)20.0F));
         HBox inputs = new HBox((double)15.0F);
         inputs.setPadding(new Insets((double)0.0F, (double)0.0F, (double)20.0F, (double)0.0F));
         TextField b = new TextField();
         b.setPromptText("باركود");
         TextField n = new TextField();
         n.setPromptText("اسم");
         TextField p = new TextField();
         p.setPromptText("سعر");
         TextField cst = new TextField();
         cst.setPromptText("تكلفة");
         TextField q = new TextField();
         q.setPromptText("كمية");
         Button add = new Button("حفظ المنتج");
         add.getStyleClass().add("button-success");
         add.setPrefHeight((double)40.0F);
         add.setOnAction((e) -> {
            try {
               Connection conn = DriverManager.getConnection("jdbc:sqlite:40dash.db");

               try {
                  PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO products VALUES(?,?,?,?,?)");

                  try {
                     ps.setString(1, b.getText());
                     ps.setString(2, n.getText());
                     ps.setDouble(3, Double.parseDouble(p.getText()));
                     ps.setDouble(4, Double.parseDouble(cst.getText()));
                     ps.setInt(5, Integer.parseInt(q.getText()));
                     ps.executeUpdate();
                     this.refresh();
                     b.clear();
                     n.clear();
                     p.clear();
                     cst.clear();
                     q.clear();
                     App.this.showToast("✅ تم التحديث", false);
                  } catch (Throwable var13) {
                     if (ps != null) {
                        try {
                           ps.close();
                        } catch (Throwable var12) {
                           var13.addSuppressed(var12);
                        }
                     }

                     throw var13;
                  }

                  if (ps != null) {
                     ps.close();
                  }
               } catch (Throwable var14) {
                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (Throwable var11) {
                        var14.addSuppressed(var11);
                     }
                  }

                  throw var14;
               }

               if (conn != null) {
                  conn.close();
               }
            } catch (Exception var15) {
            }

         });
         inputs.getChildren().addAll(b, n, p, cst, q, add);
         TableColumn<Product, String> col1 = new TableColumn<Product, String>("باركود");
         col1.setCellValueFactory(new PropertyValueFactory("barcode"));
         TableColumn<Product, String> col2 = new TableColumn<Product, String>("الاسم");
         col2.setCellValueFactory(new PropertyValueFactory("name"));
         TableColumn<Product, Integer> col3 = new TableColumn<Product, Integer>("الكمية المتاحة");
         col3.setCellValueFactory(new PropertyValueFactory("quantity"));
         this.table.getColumns().addAll(col1, col2, col3);
         this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
         this.refresh();
         this.setTop(inputs);
         this.setCenter(this.table);
      }

      void refresh() {
         this.masterData.clear();

         try {
            Connection conn = DriverManager.getConnection("jdbc:sqlite:40dash.db");

            try {
               ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM products");

               try {
                  while(rs.next()) {
                     this.masterData.add(new Product(rs.getString("barcode"), rs.getString("name"), rs.getDouble("price"), rs.getDouble("cost"), rs.getInt("quantity")));
                  }
               } catch (Throwable var7) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var8) {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (Throwable var5) {
                     var8.addSuppressed(var5);
                  }
               }

               throw var8;
            }

            if (conn != null) {
               conn.close();
            }
         } catch (Exception var9) {
         }

         this.table.setItems(this.masterData);
      }
   }

   class ExpensePane extends BorderPane {
      ObservableList<Expense> expList = FXCollections.<Expense>observableArrayList();
      TableView<Expense> table;

      ExpensePane() {
         this.table = new TableView<Expense>(this.expList);
         this.setPadding(new Insets((double)20.0F));
         HBox inputs = new HBox((double)15.0F);
         inputs.setPadding(new Insets((double)0.0F, (double)0.0F, (double)20.0F, (double)0.0F));
         TextField t = new TextField();
         t.setPromptText("بند المصروف");
         TextField a = new TextField();
         a.setPromptText("المبلغ");
         Button b = new Button("تسجيل");
         b.getStyleClass().add("button-danger");
         b.setPrefHeight((double)40.0F);
         b.setOnAction((e) -> {
            try {
               Connection conn = DriverManager.getConnection("jdbc:sqlite:40dash.db");

               try {
                  PreparedStatement ps = conn.prepareStatement("INSERT INTO expenses (date, title, amount, user) VALUES (?,?,?,?)");

                  try {
                     ps.setString(1, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
                     ps.setString(2, t.getText());
                     ps.setDouble(3, Double.parseDouble(a.getText()));
                     ps.setString(4, App.loggedUser);
                     ps.executeUpdate();
                     t.clear();
                     a.clear();
                     this.refresh();
                     App.this.showToast("\ud83d\udcb8 تم تسجيل المصروف", false);
                  } catch (Throwable var10) {
                     if (ps != null) {
                        try {
                           ps.close();
                        } catch (Throwable var9) {
                           var10.addSuppressed(var9);
                        }
                     }

                     throw var10;
                  }

                  if (ps != null) {
                     ps.close();
                  }
               } catch (Throwable var11) {
                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (Throwable var8) {
                        var11.addSuppressed(var8);
                     }
                  }

                  throw var11;
               }

               if (conn != null) {
                  conn.close();
               }
            } catch (Exception var12) {
            }

         });
         inputs.getChildren().addAll(t, a, b);
         TableColumn<Expense, String> c1 = new TableColumn<Expense, String>("التاريخ");
         c1.setCellValueFactory(new PropertyValueFactory("date"));
         TableColumn<Expense, String> c2 = new TableColumn<Expense, String>("البند");
         c2.setCellValueFactory(new PropertyValueFactory("title"));
         TableColumn<Expense, Double> c3 = new TableColumn<Expense, Double>("المبلغ");
         c3.setCellValueFactory(new PropertyValueFactory("amount"));
         TableColumn<Expense, String> c4 = new TableColumn<Expense, String>("بواسطة");
         c4.setCellValueFactory(new PropertyValueFactory("user"));
         this.table.getColumns().addAll(c1, c2, c3, c4);
         this.table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
         this.setTop(inputs);
         this.setCenter(this.table);
         this.refresh();
      }

      void refresh() {
         this.expList.clear();

         try {
            Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

            try {
               ResultSet rs = c.createStatement().executeQuery("SELECT * FROM expenses ORDER BY id DESC");

               try {
                  while(rs.next()) {
                     this.expList.add(new Expense(rs.getString("date"), rs.getString("title"), rs.getDouble("amount"), rs.getString("user")));
                  }
               } catch (Throwable var7) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var8) {
               if (c != null) {
                  try {
                     c.close();
                  } catch (Throwable var5) {
                     var8.addSuppressed(var5);
                  }
               }

               throw var8;
            }

            if (c != null) {
               c.close();
            }
         } catch (Exception var9) {
         }

      }
   }

   class DashboardPane extends VBox {
      Label sL = new Label("0.00");
      Label eL = new Label("0.00");
      Label pL = new Label("0.00");

      DashboardPane() {
         this.setPadding(new Insets((double)40.0F));
         this.setSpacing((double)30.0F);
         this.setAlignment(Pos.TOP_CENTER);
         HBox h = new HBox((double)30.0F, new Node[]{this.createCard("إجمالي المبيعات", this.sL), this.createCard("إجمالي المصاريف", this.eL), this.createCard("صافي الأرباح", this.pL)});
         h.setAlignment(Pos.CENTER);
         this.getChildren().addAll(new Label("تحليلات الأداء المالي") {
            // $FF: synthetic field
            final App val$this$0;
            // $FF: synthetic field
            final App.DashboardPane this$1;

            {
               super(arg0);
               this.this$1 = this$1;
               this.val$this$0 = var3;
               this.setFont(Font.font("System", FontWeight.BOLD, (double)24.0F));
            }
         }, h);
         this.refreshStats();
      }

      void refreshStats() {
         double s = (double)0.0F;
         double p = (double)0.0F;
         double e = (double)0.0F;

         try {
            Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

            try {
               ResultSet rs1 = c.createStatement().executeQuery("SELECT SUM(total), SUM(profit) FROM orders");
               if (rs1.next()) {
                  s = rs1.getDouble(1);
                  p = rs1.getDouble(2);
               }

               ResultSet rs2 = c.createStatement().executeQuery("SELECT SUM(amount) FROM expenses");
               if (rs2.next()) {
                  e = rs2.getDouble(1);
               }
            } catch (Throwable var11) {
               if (c != null) {
                  try {
                     c.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (c != null) {
               c.close();
            }
         } catch (Exception var12) {
         }

         this.sL.setText(String.format("%.2f", s));
         this.eL.setText(String.format("%.2f", e));
         this.pL.setText(String.format("%.2f", p - e));
      }

      VBox createCard(String t, Label v) {
         VBox vb = new VBox((double)15.0F, new Node[]{new Label(t) {
            // $FF: synthetic field
            final App.DashboardPane this$1;

            {
               super(arg0);
               this.this$1 = this$1;
               this.setFont(Font.font((double)18.0F));
            }
         }, v});
         vb.getStyleClass().add("card");
         v.setStyle("-fx-font-size: 32px; -fx-font-weight: bold;");
         vb.setMinWidth((double)300.0F);
         vb.setAlignment(Pos.CENTER);
         return vb;
      }
   }

   class UsersPane extends VBox {
      ObservableList<UserData> uList = FXCollections.<UserData>observableArrayList();
      TableView<UserData> uTable;

      UsersPane() {
         this.uTable = new TableView<UserData>(this.uList);
         this.setPadding(new Insets((double)20.0F));
         this.setSpacing((double)15.0F);
         HBox inputs = new HBox((double)15.0F);
         TextField uF = new TextField();
         uF.setPromptText("المستخدم");
         PasswordField pF = new PasswordField();
         pF.setPromptText("باسورد");
         ComboBox<String> rB = new ComboBox<String>(FXCollections.observableArrayList("ADMIN", "CASHIER"));
         rB.setValue("CASHIER");
         Button addB = new Button("حفظ الموظف");
         addB.getStyleClass().add("button-success");
         addB.setOnAction((e) -> {
            try {
               Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

               try {
                  PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO users VALUES(?,?,?)");

                  try {
                     ps.setString(1, uF.getText());
                     ps.setString(2, App.hashPassword(pF.getText()));
                     ps.setString(3, (String)rB.getValue());
                     ps.executeUpdate();
                     this.refreshU();
                     uF.clear();
                     pF.clear();
                     App.this.showToast("✅ تم حفظ المستخدم (مشفر)", false);
                  } catch (Throwable var11) {
                     if (ps != null) {
                        try {
                           ps.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  if (ps != null) {
                     ps.close();
                  }
               } catch (Throwable var12) {
                  if (c != null) {
                     try {
                        c.close();
                     } catch (Throwable var9) {
                        var12.addSuppressed(var9);
                     }
                  }

                  throw var12;
               }

               if (c != null) {
                  c.close();
               }
            } catch (Exception var13) {
            }

         });
         inputs.getChildren().addAll(uF, pF, rB, addB);
         TableColumn<UserData, String> c1 = new TableColumn<UserData, String>("الاسم");
         c1.setCellValueFactory(new PropertyValueFactory("username"));
         TableColumn<UserData, String> c2 = new TableColumn<UserData, String>("الصلاحية");
         c2.setCellValueFactory(new PropertyValueFactory("role"));
         this.uTable.getColumns().addAll(c1, c2);
         this.uTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
         this.getChildren().addAll(new Label("إدارة صلاحيات الموظفين"), inputs, this.uTable);
         this.refreshU();
      }

      void refreshU() {
         this.uList.clear();

         try {
            Connection c = DriverManager.getConnection("jdbc:sqlite:40dash.db");

            try {
               ResultSet rs = c.createStatement().executeQuery("SELECT username, role FROM users");

               try {
                  while(rs.next()) {
                     this.uList.add(new UserData(rs.getString(1), rs.getString(2)));
                  }
               } catch (Throwable var7) {
                  if (rs != null) {
                     try {
                        rs.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var8) {
               if (c != null) {
                  try {
                     c.close();
                  } catch (Throwable var5) {
                     var8.addSuppressed(var5);
                  }
               }

               throw var8;
            }

            if (c != null) {
               c.close();
            }
         } catch (Exception var9) {
         }

      }
   }

   public static class UserData {
      private String username;
      private String role;

      public UserData(String u, String r) {
         this.username = u;
         this.role = r;
      }

      public String getUsername() {
         return this.username;
      }

      public String getRole() {
         return this.role;
      }
   }

   public static class Expense {
      private String date;
      private String title;
      private String user;
      private double amount;

      public Expense(String d, String t, double a, String u) {
         this.date = d;
         this.title = t;
         this.amount = a;
         this.user = u;
      }

      public String getDate() {
         return this.date;
      }

      public String getTitle() {
         return this.title;
      }

      public double getAmount() {
         return this.amount;
      }

      public String getUser() {
         return this.user;
      }
   }

   public static class Product {
      private String barcode;
      private String name;
      private double price;
      private double cost;
      private int quantity;

      public Product(String b, String n, double p, double c, int q) {
         this.barcode = b;
         this.name = n;
         this.price = p;
         this.cost = c;
         this.quantity = q;
      }

      public String getBarcode() {
         return this.barcode;
      }

      public String getName() {
         return this.name;
      }

      public double getPrice() {
         return this.price;
      }

      public double getCost() {
         return this.cost;
      }

      public int getQuantity() {
         return this.quantity;
      }
   }

   public static class SaleItem {
      private String barcode;
      private String name;
      private IntegerProperty qty;
      private double price;
      private double cost;
      private DoubleProperty total;

      public SaleItem(String b, String n, double p, double c, int q) {
         this.barcode = b;
         this.name = n;
         this.price = p;
         this.cost = c;
         this.qty = new SimpleIntegerProperty(q);
         this.total = new SimpleDoubleProperty(p * (double)q);
      }

      public String getBarcode() {
         return this.barcode;
      }

      public String getName() {
         return this.name;
      }

      public int getQty() {
         return this.qty.get();
      }

      public void setQty(int q) {
         this.qty.set(q);
         this.total.set((double)q * this.price);
      }

      public double getCost() {
         return this.cost;
      }

      public double getTotal() {
         return this.total.get();
      }

      public IntegerProperty qtyProperty() {
         return this.qty;
      }

      public DoubleProperty totalProperty() {
         return this.total;
      }
   }
}
