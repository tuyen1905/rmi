import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;

public class LoginFrame extends JFrame {
    private JTextField txtServerIp, txtUser;
    private JPasswordField txtPass;
    private JButton btnConnect, btnLogin, btnRegister;
    private JLabel lblAuthStatus;

    private SyncService currentService;

    public LoginFrame() {
        setTitle("RMI Client - Đăng Nhập / Đăng Ký");
        setSize(420, 360);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(240, 244, 248));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("ĐĂNG NHẬP HỆ THỐNG", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(new Color(0, 102, 204));

        txtServerIp = new JTextField("172.20.10.10", 15);
        btnConnect = new JButton("🔌 Kết nối Server");

        txtUser = new JTextField(15);
        txtPass = new JPasswordField(15);

        btnLogin = new JButton("🔑 Đăng Nhập");
        btnRegister = new JButton("📝 Đăng Ký Mới");

        btnLogin.setEnabled(false);
        btnRegister.setEnabled(false);
        btnLogin.setBackground(new Color(40, 167, 69));
        btnLogin.setForeground(Color.WHITE);

        lblAuthStatus = new JLabel("Vui lòng kết nối Server trước.", SwingConstants.CENTER);
        lblAuthStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        panel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("IP Server RMI:"), gbc);
        gbc.gridx = 1; panel.add(txtServerIp, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(btnConnect, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 3; panel.add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1; panel.add(txtUser, gbc);

        gbc.gridx = 0; gbc.gridy = 4; panel.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1; panel.add(txtPass, gbc);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnBox.setOpaque(false);
        btnBox.add(btnLogin);
        btnBox.add(btnRegister);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        panel.add(btnBox, gbc);

        gbc.gridy = 6;
        panel.add(lblAuthStatus, gbc);

        add(panel);

        btnConnect.addActionListener(e -> connectToServer());
        btnLogin.addActionListener(e -> handleLogin());
        btnRegister.addActionListener(e -> handleRegister());
    }

    private void connectToServer() {
        String ip = txtServerIp.getText().trim();
        try {
            String url = "rmi://" + ip + ":1099/SyncService";
            currentService = (SyncService) Naming.lookup(url);

            lblAuthStatus.setText("✅ Đã kết nối tới Server: " + ip);
            btnLogin.setEnabled(true);
            btnRegister.setEnabled(true);
            btnConnect.setEnabled(false);
            txtServerIp.setEditable(false);
        } catch (Exception ex) {
            lblAuthStatus.setText("❌ Không thể kết nối Server!");
            JOptionPane.showMessageDialog(this, "Không thể kết nối Server RMI!\nKiểm tra lại IP hoặc Tường lửa.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogin() {
        String u = txtUser.getText().trim();
        String p = new String(txtPass.getPassword()).trim();

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Username và Password!");
            return;
        }

        try {
            boolean success = currentService.login(u, p);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đăng nhập thành công! Chào mừng " + u);

                // Mở giao diện chính và đóng giao diện Đăng nhập
                new ClientGUI(currentService, u).setVisible(true);
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!", "Đăng nhập thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            lblAuthStatus.setText("❌ Lỗi khi đăng nhập: " + ex.getMessage());
        }
    }

    private void handleRegister() {
        String u = txtUser.getText().trim();
        String p = new String(txtPass.getPassword()).trim();

        if (u.isEmpty() || p.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin để đăng ký!");
            return;
        }

        try {
            boolean success = currentService.registerUser(u, p);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đăng ký thành công! Tài khoản đã được tự động đồng bộ sang Server Peer.\nBây giờ bạn có thể đăng nhập.");
            } else {
                JOptionPane.showMessageDialog(this, "Tên tài khoản đã tồn tại!", "Lỗi Đăng Ký", JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception ex) {
            lblAuthStatus.setText("❌ Lỗi đăng ký: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}