package resourcesharing;

import javax.swing.*;
import resourcesharing.ResourceSharingAdmin;
import resourcesharing.ResourceSharingUser;


public class MainLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Object[] options = {"User", "Admin"};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Select your role:",
                    "Barangay Resource Sharing",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (choice == 1) { // Admin
                String password = JOptionPane.showInputDialog(null, "Enter admin password:");
                if ("admin123".equals(password)) {
                    new ResourceSharingAdmin().setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(null, "Incorrect password.");
                }
            } else if (choice == 0) { // User
                new ResourceSharingUser().setVisible(true);
            }
        });
    }
}
