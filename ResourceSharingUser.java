package resourcesharing;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ResourceSharingUser extends JFrame {
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Owner", "Category", "Available"}, 0
    ) {
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable itemTable = new JTable(tableModel);

    public ResourceSharingUser() {
        setTitle("User - Resource Browser");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(buildMainPanel());
        displayItems();
    }

    private JPanel buildMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        itemTable.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(itemTable);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> displayItems());

        JButton requestBtn = new JButton("Request Item");
        requestBtn.addActionListener(e -> requestSelectedItem());

        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> searchItems(searchField.getText().trim()));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(refreshBtn);
        topPanel.add(requestBtn);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(searchPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void displayItems() {
        tableModel.setRowCount(0);
        try (Connection conn = DBHelper.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM items");
            while (rs.next()) {
                String availability;
                if (rs.getInt("is_requested") == 1) {
                    availability = "Pending";
                } else if (rs.getInt("is_available") == 1) {
                    availability = "Yes";
                } else {
                    availability = "No";
                }

                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("owner"),
                        rs.getString("category"),
                        availability
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
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
                String availability;
                if (rs.getInt("is_requested") == 1) {
                    availability = "Pending";
                } else if (rs.getInt("is_available") == 1) {
                    availability = "Yes";
                } else {
                    availability = "No";
                }

                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("owner"),
                        rs.getString("category"),
                        availability
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Search error: " + e.getMessage());
        }
    }

    private void requestSelectedItem() {
        int selectedRow = itemTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an item to request.");
            return;
        }

        String availability = (String) tableModel.getValueAt(selectedRow, 4);
        if (!availability.equalsIgnoreCase("Yes")) {
            JOptionPane.showMessageDialog(this, "This item is not available right now.");
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Do you want to request this item?",
                "Confirm Request", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DBHelper.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE items SET is_requested = 1 WHERE id = ?");
            ps.setInt(1, id);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Item request sent! Pending admin approval.");
                displayItems();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to request item.");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }
}
