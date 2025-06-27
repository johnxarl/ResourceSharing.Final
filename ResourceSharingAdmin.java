package resourcesharing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ResourceSharingAdmin extends JFrame {
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Owner", "Category", "Available"}, 0
    ) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable itemTable = new JTable(tableModel);
    private final JTextField nameField = new JTextField(15);
    private final JTextField ownerField = new JTextField(15);
    private final JTextField categoryField = new JTextField(15);

    public ResourceSharingAdmin() {
        setTitle("Admin - Resource Manager");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("View Items", buildViewPanel());
        tabs.addTab("Add Item", buildAddPanel());
        add(tabs);

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

        JButton toggleBtn = new JButton("Toggle Status");
        toggleBtn.addActionListener(e -> toggleStatus());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        top.add(refreshBtn);
        top.add(deleteBtn);
        top.add(toggleBtn);

        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> searchItems(searchField.getText().trim()));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        panel.add(top, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildAddPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Item Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Owner:"), gbc);
        gbc.gridx = 1;
        panel.add(ownerField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        panel.add(categoryField, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        JButton addBtn = new JButton("List Item");
        addBtn.addActionListener(e -> addItem());
        panel.add(addBtn, gbc);

        return panel;
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

    private void deleteSelectedItem() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to delete.");
            return;
        }
        int id = (int) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete item ID " + id + "?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM items WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            displayItems();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void toggleStatus() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a row to toggle status.");
            return;
        }
        int id = (int) tableModel.getValueAt(selectedRow, 0);

        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE items SET is_available = CASE WHEN is_available = 1 THEN 0 ELSE 1 END WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            displayItems();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    private void searchItems(String query) {
        tableModel.setRowCount(0);
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
                String categoryOnly = query.replace("category:", "").trim();
                ps.setString(1, "%" + categoryOnly.toLowerCase() + "%");
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
}
