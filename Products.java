
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Products extends JPanel {

    private JTextField nameField, quantityField, priceField, searchField;
    private JTable table;
    private DefaultTableModel model;

    public Products() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(242, 246, 252));
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        JLabel heading = new JLabel("Product Management");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.setForeground(new Color(35, 48, 70));

        JLabel subtitle = new JLabel("Manage warehouse inventory");
        subtitle.setForeground(new Color(110, 124, 148));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setOpaque(false);
        titlePanel.add(heading);
        titlePanel.add(subtitle);

        JPanel form = new JPanel(new GridLayout(2, 3, 12, 10));
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        nameField = new JTextField();
        quantityField = new JTextField();
        priceField = new JTextField();

        form.add(createField("Product Name", nameField));
        form.add(createField("Quantity", quantityField));
        form.add(createField("Price", priceField));

        JButton addButton = createButton("Add Product", new Color(48, 105, 230));
        JButton updateButton = createButton("Update Selected", new Color(15, 157, 132));
        JButton deleteButton = createButton("Delete Selected", new Color(210, 65, 75));

        form.add(addButton);
        form.add(updateButton);
        form.add(deleteButton);

        model = new DefaultTableModel(
                new String[]{"ID", "Product Name", "Quantity", "Price"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(230, 237, 248));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        searchField = new JTextField();
        JButton searchButton = createButton("Search", new Color(48, 105, 230));
        JButton refreshButton = createButton("Refresh", new Color(80, 95, 120));

        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchButtons.setOpaque(false);
        searchButtons.add(searchButton);
        searchButtons.add(refreshButton);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        JPanel top = new JPanel(new BorderLayout(0, 18));
        top.setOpaque(false);
        top.add(titlePanel, BorderLayout.NORTH);
        top.add(form, BorderLayout.CENTER);
        top.add(searchPanel, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        addButton.addActionListener(e -> addProduct());
        updateButton.addActionListener(e -> updateProduct());
        deleteButton.addActionListener(e -> deleteProduct());
        searchButton.addActionListener(e -> loadProducts(searchField.getText().trim()));
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            loadProducts("");
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
                int row = table.getSelectedRow();
                nameField.setText(model.getValueAt(row, 1).toString());
                quantityField.setText(model.getValueAt(row, 2).toString());
                priceField.setText(model.getValueAt(row, 3).toString());
            }
        });

        loadProducts("");
    }

    private JPanel createField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);

        JLabel title = new JLabel(label);
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));

        panel.add(title, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void loadProducts(String search) {
        model.setRowCount(0);

        String sql = "SELECT product_id, product_name, quantity, price " +
                     "FROM products WHERE product_name LIKE ? ORDER BY product_id";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + search + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getInt("product_id"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("price")
                    });
                }
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not load products:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean validateInput() {
        try {
            String name = nameField.getText().trim();
            int quantity = Integer.parseInt(quantityField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());

            if (name.isEmpty() || quantity < 0 ||
                    !Double.isFinite(price) || price < 0) {
                throw new IllegalArgumentException();
            }
            return true;

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    "Enter a product name, non-negative quantity and valid price.");
            return false;
        }
    }

    private void addProduct() {
        if (!validateInput()) return;

        String sql = "INSERT INTO products (product_name, quantity, price) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nameField.getText().trim());
            ps.setInt(2, Integer.parseInt(quantityField.getText().trim()));
            ps.setBigDecimal(3, new java.math.BigDecimal(priceField.getText().trim()));
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Product added successfully!");
            clearFields();
            loadProducts("");

        } catch (SQLException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateProduct() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product from the table first.");
            return;
        }
        if (!validateInput()) return;

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());
        String sql = "UPDATE products SET product_name=?, quantity=?, price=? WHERE product_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, nameField.getText().trim());
            ps.setInt(2, Integer.parseInt(quantityField.getText().trim()));
            ps.setBigDecimal(3, new java.math.BigDecimal(priceField.getText().trim()));
            ps.setInt(4, id);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Product updated successfully!");
            clearFields();
            loadProducts("");

        } catch (SQLException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteProduct() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product to delete.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this product?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) return;

        int id = Integer.parseInt(model.getValueAt(row, 0).toString());

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM products WHERE product_id=?")) {

            ps.setInt(1, id);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Product deleted successfully!");
            clearFields();
            loadProducts("");

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not delete product. It may be referenced by a dispatch or return.",
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        nameField.setText("");
        quantityField.setText("");
        priceField.setText("");
        table.clearSelection();
    }
}
