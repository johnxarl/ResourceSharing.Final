package resourcesharing;

import javax.swing.*;

public class ResourceSharingLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Object[] options = {"User", "Admin"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Login as:",
                    "Barangay Resource Sharing Platform",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 1) { // Admin
                String password = JOptionPane.showInputDialog(null, "Enter Admin Password:");
                if ("admin123".equals(password)) {
                    new ResourceSharingAdmin().setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Wrong password. Exiting.");
                }
            } else if (choice == 0) { // User
                new ResourceSharingUser().setVisible(true);
            }
        });
    }
}
