package client;

import common.interfaces.AuthService;
import common.interfaces.HRMService;
import common.interfaces.PRSService;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.ConnectException;
import java.rmi.ConnectIOException;
import java.rmi.MarshalException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import javax.swing.*;
import java.awt.*;

/**
 * Client entry point with dark mode, password change, and
 * TRANSPARENT PRIMARY-BACKUP FAILOVER.
 *
 * Usage:
 *   Single server:   java client.ClientMain [host] [port]
 *   With failover:   java client.ClientMain [primaryHost] [primaryPort] [backupHost] [backupPort]
 *
 * When 4 args are provided, all remote calls go through failover proxies.
 * If the primary server dies, the client auto-switches to the backup,
 * shows a popup, and redirects to the login screen.
 * All panel classes (Employee, HR, Admin, Login) require ZERO modifications.
 */
public class ClientMain {

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    // ==================== DARK THEME COLORS ====================
    public static final Color BG_DARK       = new Color(30, 30, 36);
    public static final Color BG_PANEL      = new Color(40, 42, 50);
    public static final Color BG_CARD       = new Color(50, 53, 63);
    public static final Color BG_INPUT      = new Color(55, 58, 68);
    public static final Color BG_HOVER      = new Color(65, 68, 80);
    public static final Color FG_PRIMARY    = new Color(220, 222, 230);
    public static final Color FG_SECONDARY  = new Color(150, 154, 170);
    public static final Color FG_DIM        = new Color(100, 104, 120);
    public static final Color ACCENT_BLUE   = new Color(86, 140, 245);
    public static final Color ACCENT_GREEN  = new Color(72, 199, 142);
    public static final Color ACCENT_RED    = new Color(235, 87, 87);
    public static final Color ACCENT_ORANGE = new Color(242, 163, 60);
    public static final Color ACCENT_PURPLE = new Color(158, 106, 230);
    public static final Color BORDER_COLOR  = new Color(60, 63, 75);
    public static final Color TABLE_ROW_ALT = new Color(45, 48, 58);
    public static final Color TABLE_SEL     = new Color(86, 140, 245, 50);

    public static AuthService authService;
    public static HRMService hrmService;
    public static PRSService prsService;
    public static String sessionToken;
    public static JFrame mainFrame;

    // ==================== FAILOVER STATE ====================
    private static String primaryHost;
    private static int primaryPort;
    private static String backupHost;
    private static int backupPort;
    private static boolean onBackup = false;
    private static boolean failoverEnabled = false;
    private static final Object failoverLock = new Object();
    private static FailoverHandler authHandler, hrmHandler, prsHandler;

    public static void main(String[] args) {
        primaryHost = args.length >= 1 ? args[0] : "localhost";
        primaryPort = 1099;
        if (args.length >= 2) {
            try { primaryPort = Integer.parseInt(args[1]); } catch (NumberFormatException e) {}
        }
        if (args.length >= 4) {
            backupHost = args[2];
            try { backupPort = Integer.parseInt(args[3]); } catch (NumberFormatException e) {}
            failoverEnabled = true;
            System.out.println("[CLIENT] Failover enabled: backup at " + backupHost + ":" + backupPort);
        }

        boolean connected = connectToServer(primaryHost, primaryPort, "primary");
        if (!connected && failoverEnabled) {
            System.out.println("[CLIENT] Primary unavailable, trying backup...");
            connected = connectToServer(backupHost, backupPort, "backup");
            if (connected) onBackup = true;
        }

        if (!connected) {
            String msg = "Cannot connect to server at " + primaryHost + ":" + primaryPort;
            if (failoverEnabled) msg += "\nBackup at " + backupHost + ":" + backupPort + " also unreachable.";
            JOptionPane.showMessageDialog(null, msg, "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> {
            applyDarkTheme();
            mainFrame = new JFrame(buildTitle("Login"));
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setSize(1050, 720);
            mainFrame.setMinimumSize(new Dimension(900, 600));
            mainFrame.setLocationRelativeTo(null);
            mainFrame.getContentPane().setBackground(BG_DARK);
            showLoginPanel();
            mainFrame.setVisible(true);
        });
    }

    private static boolean connectToServer(String host, int port, String label) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("[CLIENT] Connecting to " + label + " (" + host + ":" + port + ") attempt " + attempt + "...");
                Registry registry;
                if ("true".equals(System.getProperty("ssl.enabled"))) {
                    registry = LocateRegistry.getRegistry(host, port, new javax.rmi.ssl.SslRMIClientSocketFactory());
                    System.out.println("[CLIENT] Using SSL/TLS connection");
                } else {
                    registry = LocateRegistry.getRegistry(host, port);
                }
                AuthService rawAuth = (AuthService) registry.lookup("AuthService");
                HRMService rawHRM = (HRMService) registry.lookup("HRMService");
                PRSService rawPRS = (PRSService) registry.lookup("PRSService");
                rawAuth.getCurrentUser("test");

                if (failoverEnabled) {
                    authHandler = new FailoverHandler(rawAuth);
                    hrmHandler  = new FailoverHandler(rawHRM);
                    prsHandler  = new FailoverHandler(rawPRS);
                    authService = proxyFor(AuthService.class, authHandler);
                    hrmService  = proxyFor(HRMService.class, hrmHandler);
                    prsService  = proxyFor(PRSService.class, prsHandler);
                } else {
                    authService = rawAuth;
                    hrmService  = rawHRM;
                    prsService  = rawPRS;
                }
                System.out.println("[CLIENT] Connected to " + label + "!");
                return true;
            } catch (Exception e) {
                System.err.println("[CLIENT] " + label + " attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) { break; }
                }
            }
        }
        return false;
    }

    // ==================== FAILOVER LOGIC ====================

    private static class FailoverHandler implements InvocationHandler {
        private volatile Object service;
        FailoverHandler(Object service) { this.service = service; }
        void updateService(Object s) { this.service = s; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(service, args);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (isConnectionError(cause)) {
                    System.err.println("[FAILOVER] Connection error: " + cause.getClass().getSimpleName());
                    if (attemptFailover()) {
                        throw new RemoteException("Server failover completed. Please login again.");
                    } else {
                        throw new RemoteException("Server unreachable and backup not available.");
                    }
                }
                throw cause;
            }
        }
    }

    private static boolean attemptFailover() {
        synchronized (failoverLock) {
            if (onBackup || backupHost == null) return false;
            System.out.println("[FAILOVER] Switching to backup: " + backupHost + ":" + backupPort);
            try {
                Registry reg;
                if ("true".equals(System.getProperty("ssl.enabled"))) {
                    reg = LocateRegistry.getRegistry(backupHost, backupPort, new javax.rmi.ssl.SslRMIClientSocketFactory());
                } else {
                    reg = LocateRegistry.getRegistry(backupHost, backupPort);
                }
                AuthService newAuth = (AuthService) reg.lookup("AuthService");
                HRMService newHRM  = (HRMService) reg.lookup("HRMService");
                PRSService newPRS  = (PRSService) reg.lookup("PRSService");
                newAuth.getCurrentUser("failover-test");

                authHandler.updateService(newAuth);
                hrmHandler.updateService(newHRM);
                prsHandler.updateService(newPRS);
                sessionToken = null;
                onBackup = true;

                SwingUtilities.invokeLater(() -> {
                    if (mainFrame != null) {
                        mainFrame.setTitle(buildTitle("Login"));
                        JOptionPane.showMessageDialog(mainFrame,
                            "Primary server is down.\nSwitched to backup at " + backupHost + ":" + backupPort + ".\nPlease login again.",
                            "Server Failover", JOptionPane.WARNING_MESSAGE);
                        showLoginPanel();
                    }
                });
                return true;
            } catch (Exception e) {
                System.err.println("[FAILOVER] Backup also unreachable: " + e.getMessage());
                return false;
            }
        }
    }

    private static boolean isConnectionError(Throwable t) {
        if (t == null) return false;
        if (t instanceof ConnectException || t instanceof ConnectIOException
            || t instanceof UnmarshalException || t instanceof NoSuchObjectException
            || t instanceof MarshalException || t instanceof java.net.ConnectException
            || t instanceof java.io.EOFException) return true;
        return isConnectionError(t.getCause());
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxyFor(Class<T> iface, FailoverHandler handler) {
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[]{iface}, handler);
    }

    private static String buildTitle(String section) {
        return "BHEL HRM - " + section + (onBackup ? "  [BACKUP SERVER]" : "");
    }

    // ==================== DARK THEME ====================

    private static void applyDarkTheme() {
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("OptionPane.background", BG_PANEL);
        UIManager.put("OptionPane.messageForeground", FG_PRIMARY);
        UIManager.put("TextField.background", BG_INPUT);
        UIManager.put("TextField.foreground", FG_PRIMARY);
        UIManager.put("TextField.caretForeground", FG_PRIMARY);
        UIManager.put("PasswordField.background", BG_INPUT);
        UIManager.put("PasswordField.foreground", FG_PRIMARY);
        UIManager.put("PasswordField.caretForeground", FG_PRIMARY);
        UIManager.put("TextArea.background", BG_INPUT);
        UIManager.put("TextArea.foreground", FG_PRIMARY);
        UIManager.put("TextArea.caretForeground", FG_PRIMARY);
        UIManager.put("ComboBox.background", BG_INPUT);
        UIManager.put("ComboBox.foreground", FG_PRIMARY);
        UIManager.put("ComboBox.selectionBackground", ACCENT_BLUE);
        UIManager.put("Label.foreground", FG_PRIMARY);
        UIManager.put("TabbedPane.background", BG_PANEL);
        UIManager.put("TabbedPane.foreground", FG_SECONDARY);
        UIManager.put("TabbedPane.selected", BG_CARD);
        UIManager.put("TabbedPane.contentAreaColor", BG_DARK);
        UIManager.put("TitledBorder.titleColor", FG_SECONDARY);
        UIManager.put("Table.background", BG_PANEL);
        UIManager.put("Table.foreground", FG_PRIMARY);
        UIManager.put("Table.selectionBackground", TABLE_SEL);
        UIManager.put("Table.gridColor", BORDER_COLOR);
        UIManager.put("TableHeader.background", BG_CARD);
        UIManager.put("TableHeader.foreground", FG_SECONDARY);
        UIManager.put("ScrollPane.background", BG_DARK);
        UIManager.put("Viewport.background", BG_PANEL);
        UIManager.put("CheckBox.background", BG_PANEL);
        UIManager.put("CheckBox.foreground", FG_PRIMARY);
        UIManager.put("Button.background", BG_CARD);
        UIManager.put("Button.foreground", FG_PRIMARY);
        UIManager.put("Separator.foreground", BORDER_COLOR);
    }

    // ==================== UI HELPERS ====================

    public static JButton styledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 20, 34));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    public static JButton subtleButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_CARD); btn.setForeground(FG_SECONDARY);
        btn.setFocusPainted(false); btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(BG_HOVER); btn.setForeground(FG_PRIMARY); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(BG_CARD); btn.setForeground(FG_SECONDARY); }
        });
        return btn;
    }

    public static JTextField styledField(int cols) {
        JTextField f = new JTextField(cols);
        f.setBackground(BG_INPUT); f.setForeground(FG_PRIMARY); f.setCaretColor(FG_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return f;
    }

    public static JPasswordField styledPasswordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.setBackground(BG_INPUT); f.setForeground(FG_PRIMARY); f.setCaretColor(FG_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return f;
    }

    public static JPanel createTopBar(String titleText, Color accentColor) {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); left.setOpaque(false);
        JLabel dot = new JLabel("* ");
        dot.setForeground(accentColor); dot.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        left.add(dot);
        JLabel title = new JLabel(titleText);
        title.setForeground(FG_PRIMARY); title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        left.add(title);
        if (onBackup) {
            JLabel tag = new JLabel("  [BACKUP]");
            tag.setForeground(ACCENT_ORANGE); tag.setFont(new Font("Segoe UI", Font.BOLD, 12));
            left.add(tag);
        }
        topBar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0)); right.setOpaque(false);
        JButton pwBtn = subtleButton("Change Password");
        pwBtn.addActionListener(e -> showChangePasswordDialog());
        right.add(pwBtn);
        JButton logoutBtn = styledButton("Logout", ACCENT_RED);
        logoutBtn.setPreferredSize(new Dimension(90, 30));
        logoutBtn.addActionListener(e -> logout());
        right.add(logoutBtn);
        topBar.add(right, BorderLayout.EAST);

        return topBar;
    }

    public static void showChangePasswordDialog() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_PANEL);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8); g.fill = GridBagConstraints.HORIZONTAL; g.gridx = 0;

        JLabel h = new JLabel("Change Password"); h.setFont(new Font("Segoe UI", Font.BOLD, 14)); h.setForeground(FG_PRIMARY);
        g.gridy = 0; panel.add(h, g);
        g.gridy = 1; JLabel l1 = new JLabel("CURRENT PASSWORD"); l1.setFont(new Font("Segoe UI", Font.BOLD, 10)); l1.setForeground(FG_DIM); panel.add(l1, g);
        JPasswordField oldPw = styledPasswordField(20); g.gridy = 2; panel.add(oldPw, g);
        g.gridy = 3; JLabel l2 = new JLabel("NEW PASSWORD (min 6 chars)"); l2.setFont(new Font("Segoe UI", Font.BOLD, 10)); l2.setForeground(FG_DIM); panel.add(l2, g);
        JPasswordField newPw = styledPasswordField(20); g.gridy = 4; panel.add(newPw, g);
        g.gridy = 5; JLabel l3 = new JLabel("CONFIRM NEW PASSWORD"); l3.setFont(new Font("Segoe UI", Font.BOLD, 10)); l3.setForeground(FG_DIM); panel.add(l3, g);
        JPasswordField confirmPw = styledPasswordField(20); g.gridy = 6; panel.add(confirmPw, g);

        int result = JOptionPane.showConfirmDialog(mainFrame, panel, "Change Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String oldP = new String(oldPw.getPassword());
            String newP = new String(newPw.getPassword());
            String confP = new String(confirmPw.getPassword());
            if (newP.length() < 6) { showError("New password must be at least 6 characters."); return; }
            if (!newP.equals(confP)) { showError("Passwords do not match."); return; }
            try {
                boolean ok = authService.changePassword(oldP, newP, sessionToken);
                if (ok) showSuccess("Password changed successfully!");
                else showError("Incorrect current password.");
            } catch (Exception ex) { showError("Error: " + ex.getMessage()); }
        }
    }

    // ==================== NAVIGATION ====================
    public static void showLoginPanel()         { swap(new LoginPanel());       mainFrame.setTitle(buildTitle("Login")); }
    public static void showEmployeePanel(int id){ swap(new EmployeePanel(id));  mainFrame.setTitle(buildTitle("Employee")); }
    public static void showHRPanel()            { swap(new HRPanel());          mainFrame.setTitle(buildTitle("HR")); }
    public static void showAdminPanel()         { swap(new AdminPanel());       mainFrame.setTitle(buildTitle("Admin")); }

    private static void swap(JPanel panel) {
        mainFrame.getContentPane().removeAll();
        mainFrame.getContentPane().add(panel);
        mainFrame.revalidate(); mainFrame.repaint();
    }

    public static void logout() {
        try { if (sessionToken != null) { authService.logout(sessionToken); sessionToken = null; } }
        catch (Exception e) { System.err.println("[CLIENT] Logout error: " + e.getMessage()); }
        showLoginPanel();
    }

    public static void showError(String msg)   { JOptionPane.showMessageDialog(mainFrame, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    public static void showSuccess(String msg)  { JOptionPane.showMessageDialog(mainFrame, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
}
