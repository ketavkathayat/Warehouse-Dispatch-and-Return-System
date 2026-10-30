
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class Main {

    static Color navy = new Color(20, 32, 58);
    static Color blue = new Color(48, 105, 230);
    static Color background = new Color(242, 246, 252);
    static Color textColor = new Color(35, 48, 70);

    static JPanel content;
    static CardLayout cardLayout;

    public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> new Login().setVisible(true));
}


public static int getCount(String sql) {
    try (
        java.sql.Connection con = DBConnection.getConnection();
        java.sql.Statement st = con.createStatement();
        java.sql.ResultSet rs = st.executeQuery(sql)
    ) {
        if (rs.next()) {
            return rs.getInt(1);
        }
    } catch (java.sql.SQLException e) {
        System.out.println("Dashboard error: " + e.getMessage());
    }

    return 0;
}

public static void refreshDashboard() {
    if (content != null) {
        content.remove(content.getComponent(0));
        content.add(createDashboard(), "Dashboard");
        content.revalidate();
        content.repaint();
    }
}



    public static void createGUI(String role) {
    JFrame frame = new JFrame("Warehouse | Dispatch & Returns - " + role);
        frame.setSize(1150, 720);
        frame.setMinimumSize(new Dimension(950, 600));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(background);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel brand = new JLabel("WAREHOUSE  /  OPERATIONS");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 19));
        brand.setForeground(navy);

        JLabel user = new JLabel(role + "  ●");
        user.setFont(new Font("Segoe UI", Font.BOLD, 12));
        user.setForeground(blue);

        header.add(brand, BorderLayout.WEST);
        header.add(user, BorderLayout.EAST);

        // Sidebar
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(navy);
        sidebar.setBorder(new EmptyBorder(25, 14, 20, 14));
        sidebar.setPreferredSize(new Dimension(215, 0));

        JLabel menuTitle = new JLabel("  MAIN MENU");
        menuTitle.setForeground(new Color(155, 174, 207));
        menuTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        menuTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuTitle.setBorder(new EmptyBorder(5, 8, 18, 0));
        sidebar.add(menuTitle);

        String[] names = {
            "Dashboard", "Products", "Dispatch Orders", "Returns"
        };

        String[] keys = {
            "Dashboard", "Products", "Dispatch Orders", "Returns"
        };

        for (int i = 0; i < names.length; i++) {
            JButton button = createMenuButton(names[i]);
            String key = keys[i];

           button.addActionListener(e -> {
    if (key.equals("Dashboard")) {
        refreshDashboard();
    }

    cardLayout.show(content, key);
    setActiveButton(sidebar, button);
});

            sidebar.add(button);
            sidebar.add(Box.createVerticalStrut(8));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel footer = new JLabel("Warehouse System v1.0");
        footer.setForeground(new Color(155, 174, 207));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(footer);

        // Main pages
        cardLayout = new CardLayout();
        content = new JPanel(cardLayout);
        content.setBackground(background);

        content.add(createDashboard(), "Dashboard");
        content.add(new Products(), "Products");
        content.add(new Dispatch(), "Dispatch Orders");
        content.add(new Returns(role), "Returns");

        frame.add(header, BorderLayout.NORTH);
        frame.add(sidebar, BorderLayout.WEST);
        frame.add(content, BorderLayout.CENTER);

        cardLayout.show(content, "Dashboard");
        frame.setVisible(true);
    }

    static JButton createMenuButton(String name) {
        JButton button = new JButton("   " + name);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(navy);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setPreferredSize(new Dimension(185, 45));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    static void setActiveButton(JPanel sidebar, JButton active) {
        for (Component component : sidebar.getComponents()) {
            if (component instanceof JButton) {
                JButton button = (JButton) component;
                button.setBackground(button == active ? blue : navy);
            }
        }
        active.setBackground(blue);
    }

    static JPanel createDashboard() {
        JPanel page = new JPanel(new BorderLayout(0, 22));
        page.setBackground(background);
        page.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);

        JLabel title = new JLabel("Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 29));
        title.setForeground(textColor);

        JLabel subtitle = new JLabel(
                "Overview of your warehouse operations");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(110, 124, 148));

        JPanel headingText = new JPanel();
        headingText.setOpaque(false);
        headingText.setLayout(new BoxLayout(headingText, BoxLayout.Y_AXIS));
        headingText.add(title);
        headingText.add(Box.createVerticalStrut(6));
        headingText.add(subtitle);

        heading.add(headingText, BorderLayout.WEST);
        page.add(heading, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setOpaque(false);

        
int totalProducts = getCount(
        "SELECT COUNT(*) FROM products");

int totalStock = getCount(
        "SELECT COALESCE(SUM(quantity), 0) FROM products");

int totalDispatches = getCount(
        "SELECT COUNT(*) FROM dispatches");

int pendingReturns = getCount(
        "SELECT COUNT(*) FROM return_requests WHERE status = 'PENDING'");

JPanel cards = new JPanel(new GridLayout(1, 4, 12, 0));
cards.setOpaque(false);
cards.setPreferredSize(new Dimension(0, 145));

cards.add(createStatCard("TOTAL PRODUCTS",
        String.valueOf(totalProducts),
        "Different products", new Color(48, 105, 230)));

cards.add(createStatCard("TOTAL STOCK",
        String.valueOf(totalStock),
        "Units in inventory", new Color(130, 90, 210)));

cards.add(createStatCard("DISPATCH ORDERS",
        String.valueOf(totalDispatches),
        "Outgoing shipments", new Color(15, 157, 132)));

cards.add(createStatCard("PENDING RETURNS",
        String.valueOf(pendingReturns),
        "Awaiting review", new Color(224, 145, 42)));


        JPanel activity = new JPanel(new BorderLayout(0, 15));
        activity.setBackground(Color.WHITE);
        activity.setBorder(new EmptyBorder(22, 22, 22, 22));

        JLabel activityTitle = new JLabel("Quick Actions");
        activityTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
        activityTitle.setForeground(textColor);

        JPanel actions = new JPanel(new GridLayout(3, 1, 0, 12));
        actions.setOpaque(false);

        actions.add(createActionRow("01", "Product Management",
                "Add, update and search inventory"));
        actions.add(createActionRow("02", "Dispatch Orders",
                "Prepare and track shipments"));
        actions.add(createActionRow("03", "Return Authorization",
                "Review and process returned items"));

        activity.add(activityTitle, BorderLayout.NORTH);
        activity.add(actions, BorderLayout.CENTER);

        body.add(cards, BorderLayout.NORTH);
        body.add(activity, BorderLayout.CENTER);

        page.add(body, BorderLayout.CENTER);
        return page;
    }

    static JPanel createStatCard(String heading, String value,
                                 String description, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accent),
                new EmptyBorder(17, 17, 17, 17)));

        JLabel h = new JLabel(heading);
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
        h.setForeground(new Color(110, 124, 148));

        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 30));
        v.setForeground(textColor);

        JLabel d = new JLabel(description);
        d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        d.setForeground(new Color(110, 124, 148));

        JPanel middle = new JPanel(new BorderLayout());
        middle.setOpaque(false);
        middle.add(v, BorderLayout.WEST);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(d, BorderLayout.WEST);

        card.add(h, BorderLayout.NORTH);
        card.add(middle, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    static JPanel createActionRow(String number, String title,
                                  String description) {
        JPanel row = new JPanel(new BorderLayout(15, 0));
        row.setBackground(new Color(247, 249, 253));
        row.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel num = new JLabel(number);
        num.setFont(new Font("Segoe UI", Font.BOLD, 17));
        num.setForeground(blue);

        JLabel name = new JLabel(title);
        name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        name.setForeground(textColor);

        JLabel desc = new JLabel(description);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        desc.setForeground(new Color(110, 124, 148));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(name);
        labels.add(Box.createVerticalStrut(4));
        labels.add(desc);

        row.add(num, BorderLayout.WEST);
        row.add(labels, BorderLayout.CENTER);
        return row;
    }

    static JPanel createPlaceholder(String titleText, String description) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(background);
        panel.setBorder(new EmptyBorder(35, 30, 30, 30));

        JLabel title = new JLabel(titleText);
        title.setFont(new Font("Segoe UI", Font.BOLD, 25));
        title.setForeground(textColor);

        JLabel desc = new JLabel(description);
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        desc.setForeground(new Color(110, 124, 148));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(Box.createVerticalStrut(8));
        top.add(desc);

        panel.add(top, BorderLayout.NORTH);
        return panel;
    }
}


