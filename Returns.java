
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Returns extends JPanel {

    private JComboBox<String> productBox;
    private JTextField quantityField, reasonField;
    private JTable table;
    private DefaultTableModel model;

   public Returns(String role) {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(242, 246, 252));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel title = new JLabel("Return Authorization");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(new Color(35, 48, 70));

        JLabel subtitle = new JLabel(
                "Submit return requests and review their status");
        subtitle.setForeground(new Color(110, 124, 148));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setOpaque(false);
        heading.add(title);
        heading.add(subtitle);

        productBox = new JComboBox<>();
        quantityField = new JTextField();
        reasonField = new JTextField();

        JPanel form = new JPanel(new GridLayout(2, 3, 12, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        form.add(new JLabel("Product"));
        form.add(new JLabel("Return Quantity"));
        form.add(new JLabel("Reason"));
        form.add(productBox);
        form.add(quantityField);
        form.add(reasonField);

        JButton submitButton = createButton(
                "Submit Request", new Color(48, 105, 230));
        JButton approveButton = createButton(
                "Approve", new Color(15, 157, 132));
        JButton rejectButton = createButton(
                "Reject", new Color(210, 65, 75));
                approveButton.setEnabled(role.equalsIgnoreCase("ADMIN"));
rejectButton.setEnabled(role.equalsIgnoreCase("ADMIN"));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(submitButton);
        actions.add(approveButton);
        actions.add(rejectButton);

        model = new DefaultTableModel(
                new String[]{"Return ID", "Product", "Quantity",
                        "Reason", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(29);
        table.getTableHeader().setFont(
                new Font("Segoe UI", Font.BOLD, 12));

        JPanel top = new JPanel(new BorderLayout(0, 15));
        top.setOpaque(false);
        top.add(heading, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(actions, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        submitButton.addActionListener(e -> submitRequest());
        approveButton.addActionListener(e -> processRequest("APPROVED"));
        rejectButton.addActionListener(e -> processRequest("REJECTED"));

        loadProducts();
        loadRequests();
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);
        return button;
    }

    private void loadProducts() {
        productBox.removeAllItems();

        String sql = "SELECT product_id, product_name FROM products ORDER BY product_name";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                productBox.addItem(
                        rs.getInt("product_id") + " - "
                        + rs.getString("product_name"));
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void submitRequest() {
        String selected = (String) productBox.getSelectedItem();

        if (selected == null) {
            JOptionPane.showMessageDialog(this, "No products available.");
            return;
        }

        int productId;
        int quantity;
        String reason = reasonField.getText().trim();

        try {
            productId = Integer.parseInt(selected.split(" - ")[0]);
            quantity = Integer.parseInt(quantityField.getText().trim());

            if (quantity <= 0 || reason.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Enter a positive quantity and return reason.");
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Return quantity must be a whole number.");
            return;
        }

        String sql = "INSERT INTO return_requests " +
                "(product_id, quantity, reason, status) VALUES (?, ?, ?, 'PENDING')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, productId);
            ps.setInt(2, quantity);
            ps.setString(3, reason);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Return request submitted successfully!");

            quantityField.setText("");
            reasonField.setText("");
            loadRequests();

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void processRequest(String newStatus) {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                    "Select a return request first.");
            return;
        }

        int returnId = Integer.parseInt(
                model.getValueAt(row, 0).toString());

        String currentStatus = model.getValueAt(row, 4).toString();

        if (!"PENDING".equalsIgnoreCase(currentStatus)) {
            JOptionPane.showMessageDialog(this,
                    "Only pending requests can be processed.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to " + newStatus.toLowerCase()
                        + " this request?",
                "Confirm Action", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) return;

        String updateStatus = "UPDATE return_requests SET status = ? " +
                "WHERE return_id = ? AND status = 'PENDING'";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                int productId;
                int quantity;

                String requestSql = "SELECT product_id, quantity " +
                        "FROM return_requests WHERE return_id = ? " +
                        "AND status = 'PENDING' FOR UPDATE";

                try (PreparedStatement ps =
                             con.prepareStatement(requestSql)) {
                    ps.setInt(1, returnId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException(
                                    "Request is no longer pending.");
                        }
                        productId = rs.getInt("product_id");
                        quantity = rs.getInt("quantity");
                    }
                }

                if ("APPROVED".equals(newStatus)) {
                    String stockSql = "UPDATE products SET quantity = quantity + ? " +
                            "WHERE product_id = ?";

                    try (PreparedStatement ps =
                                 con.prepareStatement(stockSql)) {
                        ps.setInt(1, quantity);
                        ps.setInt(2, productId);

                        if (ps.executeUpdate() != 1) {
                            throw new SQLException("Product not found.");
                        }
                    }
                }

                try (PreparedStatement ps =
                             con.prepareStatement(updateStatus)) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, returnId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Could not update request.");
                    }
                }

                con.commit();

                JOptionPane.showMessageDialog(this,
                        "Request " + newStatus.toLowerCase() + ".");

                loadRequests();
                loadProducts();

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

    private void loadRequests() {
        model.setRowCount(0);

        String sql = "SELECT r.return_id, p.product_name, r.quantity, " +
                "r.reason, r.status FROM return_requests r " +
                "JOIN products p ON r.product_id = p.product_id " +
                "ORDER BY r.return_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("return_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getString("reason"),
                        rs.getString("status")
                });
            }

        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this,
                ex.getMessage(), "Database Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
