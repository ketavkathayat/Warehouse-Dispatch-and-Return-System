
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Dispatch extends JPanel {

    private JComboBox<String> productBox;
    private JTextField quantityField;
    private JTable table;
    private DefaultTableModel model;

    public Dispatch() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(242, 246, 252));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel title = new JLabel("Dispatch Orders");
        title.setFont(new Font("Segoe UI", Font.BOLD, 27));
        title.setForeground(new Color(35, 48, 70));

        JLabel subtitle = new JLabel("Create dispatch orders and track outgoing stock");
        subtitle.setForeground(new Color(110, 124, 148));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        productBox = new JComboBox<>();
        quantityField = new JTextField();

        JPanel form = new JPanel(new GridLayout(2, 2, 12, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        form.add(new JLabel("Select Product"));
        form.add(new JLabel("Dispatch Quantity"));
        form.add(productBox);
        form.add(quantityField);

        JButton dispatchButton = new JButton("Create Dispatch");
        dispatchButton.setBackground(new Color(48, 105, 230));
        dispatchButton.setForeground(Color.WHITE);
        dispatchButton.setFocusPainted(false);

        model = new DefaultTableModel(
                new String[]{"Dispatch ID", "Product", "Quantity", "Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(29);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(dispatchButton, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        dispatchButton.addActionListener(e -> createDispatch());

        loadProducts();
        loadDispatches();
    }

    private void loadProducts() {
        productBox.removeAllItems();

        String sql = "SELECT product_id, product_name, quantity FROM products ORDER BY product_name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                productBox.addItem(
                        rs.getInt("product_id") + " - "
                        + rs.getString("product_name")
                        + " (Stock: " + rs.getInt("quantity") + ")"
                );
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void createDispatch() {
        String selected = (String) productBox.getSelectedItem();

        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please add a product first.");
            return;
        }

        int productId;
        int quantity;

        try {
            productId = Integer.parseInt(selected.split(" - ")[0]);
            quantity = Integer.parseInt(quantityField.getText().trim());

            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this,
                        "Dispatch quantity must be greater than zero.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid whole number quantity.");
            return;
        }

        String stockSql = "SELECT quantity FROM products WHERE product_id = ? FOR UPDATE";
        String updateStockSql = "UPDATE products SET quantity = quantity - ? WHERE product_id = ?";
        String insertSql = "INSERT INTO dispatches (product_id, quantity, status) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int stock;

                try (PreparedStatement ps = con.prepareStatement(stockSql)) {
                    ps.setInt(1, productId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Product no longer exists.");
                        }
                        stock = rs.getInt("quantity");
                    }
                }

                if (quantity > stock) {
                    throw new SQLException("Insufficient stock. Available: " + stock);
                }

                try (PreparedStatement ps = con.prepareStatement(updateStockSql)) {
                    ps.setInt(1, quantity);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                    ps.setInt(1, productId);
                    ps.setInt(2, quantity);
                    ps.setString(3, "DISPATCHED");
                    ps.executeUpdate();
                }

                con.commit();

                JOptionPane.showMessageDialog(this,
                        "Dispatch created successfully!");

                quantityField.setText("");
                loadProducts();
                loadDispatches();

            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadDispatches() {
        model.setRowCount(0);

        String sql = "SELECT d.dispatch_id, p.product_name, d.quantity, " +
                     "d.dispatch_date, d.status " +
                     "FROM dispatches d JOIN products p " +
                     "ON d.product_id = p.product_id " +
                     "ORDER BY d.dispatch_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("dispatch_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getTimestamp("dispatch_date"),
                        rs.getString("status")
                });
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}
