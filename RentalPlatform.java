package resourcesharing;


import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class RentalPlatform extends JFrame {
    private final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Owner", "Category", "Available"}, 0);
    private final JTable itemTable = new JTable(tableModel);
    private final JTextField nameField = new JTextField(15);
    private final JTextField ownerField = new JTextField(15);
    private final JTextField categoryField = new JTextField(15);

    public RentalPlatform() {
        setTitle("Barangay Resource Sharing Platform");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ✅ Set a modern soft background color for the whole window
        getContentPane().setBackground(new Color(245, 248, 255)); // Light pastel blue-gray

        // Tabs setup
        JTabbedPane tabs = new JTabbedPane();

        // Build and style each panel
        JPanel viewPanel = buildViewPanel();
        JPanel addPanel = buildAddPanel();

        // ✅ Set background for individual panels
        viewPanel.setBackground(new Color(245, 248, 255));
        addPanel.setBackground(new Color(245, 248, 255));

        // Add tabs
        tabs.addTab("View Items", viewPanel);
        tabs.addTab("Add Item", addPanel);
        add(tabs);

        // Load data
        displayItems();
    }


    private JPanel buildViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        itemTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(itemTable);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> displayItems());

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.addActionListener(e -> deleteSelectedItem());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(refreshBtn);
        top.add(deleteBtn); // ✅ Add the button to the view

        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
       
        
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> searchItems(searchField.getText().trim()));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        
        panel.add(searchPanel, BorderLayout.SOUTH);
        
        return panel;
    }


    private JPanel buildAddPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 248, 255)); // Match window color

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        nameField.setBackground(Color.WHITE);
        nameField.setForeground(Color.DARK_GRAY);
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Owner:"), gbc);
        gbc.gridx = 1;
        ownerField.setBackground(Color.WHITE);
        ownerField.setForeground(Color.DARK_GRAY);
        panel.add(ownerField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        categoryField.setBackground(Color.WHITE);
        categoryField.setForeground(Color.DARK_GRAY);
        panel.add(categoryField, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        JButton addBtn = new JButton("List Item");
        addBtn.setBackground(new Color(66, 133, 244)); // Google blue
        addBtn.setForeground(Color.WHITE);
        addBtn.setFocusPainted(false);
        panel.add(addBtn, gbc);

        addBtn.addActionListener(e -> addItem());

        return panel;
    }

    private void searchItems(String query) {
        tableModel.setRowCount(0); // clear table

        if (query.isEmpty()) {
            displayItems();
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            String sql = query.toLowerCase().startsWith("category:") ?
                "SELECT * FROM items WHERE LOWER(category) LIKE ?" :
                "SELECT * FROM items WHERE LOWER(name) LIKE ? OR LOWER(category) LIKE ?";

            PreparedStatement ps = conn.prepareStatement(sql);
            if (sql.contains("OR")) {
                ps.setString(1, "%" + query.toLowerCase() + "%");
                ps.setString(2, "%" + query.toLowerCase() + "%");
            } else {
                String categoryOnly = query.toLowerCase().replace("category:", "").trim();
                ps.setString(1, "%" + categoryOnly + "%");
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("owner"),
                    rs.getString("category"),
                    rs.getInt("is_available") == 1 ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage());
        }
    }


    private void addItem() {
        String name = nameField.getText().trim();
        String owner = ownerField.getText().trim();
        String category = categoryField.getText().trim();

        if (name.isEmpty() || owner.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please complete all fields.");
            return;
        }

        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO items (name, owner, category, is_available) VALUES (?, ?, ?, 1)");
            ps.setString(1, name);
            ps.setString(2, owner);
            ps.setString(3, category);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Item added.");
            nameField.setText("");
            ownerField.setText("");
            categoryField.setText("");
            displayItems();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
    private void deleteSelectedItem() {
        int selectedRow = itemTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to delete.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this item?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int itemId = (int) tableModel.getValueAt(selectedRow, 0); // ID is in column 0

        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE id = ?");
            ps.setInt(1, itemId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(this, "Item deleted successfully.");
                displayItems(); // Refresh the table
            } else {
                JOptionPane.showMessageDialog(this, "Item not found or already deleted.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error deleting item: " + e.getMessage());
        }
    }


    private void displayItems() {
        tableModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM items");
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("owner"),
                    rs.getString("category"),
                    rs.getInt("is_available") == 1 ? "Yes" : "No"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }
    private static boolean authenticate() {
        JPasswordField pf = new JPasswordField();
        int okCxl = JOptionPane.showConfirmDialog(null, pf, "Enter Admin Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (okCxl == JOptionPane.OK_OPTION) {
            String entered = new String(pf.getPassword());
            return "admin123".equals(entered);
        }
        return false;
    }




    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.out.println("Failed to set FlatLaf look and feel");
        }

        SwingUtilities.invokeLater(() -> {
            if (authenticate()) {
                new RentalPlatform().setVisible(true);
            } else {
                JOptionPane.showMessageDialog(null, "Authentication failed. Exiting.");
            }
        });
    }
}





