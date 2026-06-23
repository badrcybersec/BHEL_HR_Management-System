package client;

import common.models.UserAccount;
import javax.swing.*;
import java.awt.*;

/**
 * Dark-themed login panel with styled fields and visual feedback.
 */
public class LoginPanel extends JPanel {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JLabel statusLabel;

    public LoginPanel() {
        setLayout(new GridBagLayout());
        setBackground(ClientMain.BG_DARK);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);

        // ---- Card container ----
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(ClientMain.BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ClientMain.BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(35, 45, 35, 45)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        // Logo icon
        JLabel icon = new JLabel("●", SwingConstants.CENTER);  // hexagon
        icon.setFont(new Font("Segoe UI", Font.BOLD, 44));
        icon.setForeground(ClientMain.ACCENT_BLUE);
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        card.add(icon, g);

        // Title
        JLabel title = new JLabel("BHEL HRM System", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(ClientMain.FG_PRIMARY);
        g.gridy = 1;
        card.add(title, g);

        // Subtitle
        JLabel subtitle = new JLabel("Distributed HRM - Java RMI", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(ClientMain.FG_DIM);
        g.gridy = 2; g.insets = new Insets(0, 5, 18, 5);
        card.add(subtitle, g);

        // Username label
        g.insets = new Insets(4, 5, 2, 5);
        JLabel userLabel = new JLabel("USERNAME");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        userLabel.setForeground(ClientMain.FG_DIM);
        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        card.add(userLabel, g);

        // Username field
        usernameField = ClientMain.styledField(22);
        g.gridy = 4; g.insets = new Insets(0, 5, 10, 5);
        card.add(usernameField, g);

        // Password label
        g.insets = new Insets(4, 5, 2, 5);
        JLabel passLabel = new JLabel("PASSWORD");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        passLabel.setForeground(ClientMain.FG_DIM);
        g.gridy = 5;
        card.add(passLabel, g);

        // Password field
        passwordField = ClientMain.styledPasswordField(22);
        g.gridy = 6; g.insets = new Insets(0, 5, 16, 5);
        card.add(passwordField, g);

        // Login button
        loginButton = ClientMain.styledButton("  Sign In  ", ClientMain.ACCENT_BLUE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(0, 40));
        g.gridy = 7; g.insets = new Insets(0, 5, 8, 5);
        card.add(loginButton, g);

        // Status label
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(ClientMain.ACCENT_RED);
        g.gridy = 8; g.insets = new Insets(2, 5, 8, 5);
        card.add(statusLabel, g);

        // Default credentials hint
        JLabel hint = new JLabel(
            "<html><center><span style='color:#646878'>admin/admin123 - hr1/hr1234 - ahmad.ibrahim/emp123</span></center></html>",
            SwingConstants.CENTER);
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        g.gridy = 9;
        card.add(hint, g);

        // Add card to center of main panel
        add(card);

        // Action listeners
        loginButton.addActionListener(e -> performLogin());
        passwordField.addActionListener(e -> performLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());
    }

    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Please enter both username and password");
            statusLabel.setForeground(ClientMain.ACCENT_ORANGE);
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");
        statusLabel.setText(" ");

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ClientMain.authService.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    String token = get();
                    if (token != null) {
                        ClientMain.sessionToken = token;
                        UserAccount user = ClientMain.authService.getCurrentUser(token);

                        switch (user.getRole()) {
                            case "EMPLOYEE":
                                ClientMain.showEmployeePanel(user.getEmployeeId());
                                break;
                            case "HR":
                                ClientMain.showHRPanel();
                                break;
                            case "ADMIN":
                                ClientMain.showAdminPanel();
                                break;
                            default:
                                statusLabel.setText("Unknown role: " + user.getRole());
                                loginButton.setEnabled(true);
                                loginButton.setText("  Sign In  ");
                        }
                    } else {
                        statusLabel.setText("Invalid username or password");
                        statusLabel.setForeground(ClientMain.ACCENT_RED);
                        passwordField.setText("");
                        loginButton.setEnabled(true);
                        loginButton.setText("  Sign In  ");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Connection error: " + ex.getMessage());
                    statusLabel.setForeground(ClientMain.ACCENT_RED);
                    loginButton.setEnabled(true);
                    loginButton.setText("  Sign In  ");
                }
            }
        }.execute();
    }
}
