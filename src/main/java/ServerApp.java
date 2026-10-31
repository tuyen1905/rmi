import javax.swing.*;
import java.awt.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;

public class ServerApp extends JFrame {
    private JTextField txtMyIp;
    private JTextField txtPeerIp;
    private JButton btnStart;
    private JTextArea txtLog;

    public ServerApp() {
        setTitle("RMI Server - Trình Đồng Bộ Dữ Liệu");
        setSize(550, 420);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 1. Khung cấu hình IP
        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 8));

        formPanel.add(new JLabel("IP Máy này (Tự động phát hiện):"));
        txtMyIp = new JTextField(getAutoLocalIp());
        // Vẫn cho phép sửa nếu Java chọn nhầm Card mạng ảo
        txtMyIp.setToolTipText("Bạn có thể chỉnh lại nếu máy có nhiều Card mạng");
        formPanel.add(txtMyIp);

        formPanel.add(new JLabel("IP Server Peer (Server còn lại):"));
        txtPeerIp = new JTextField("");
        formPanel.add(txtPeerIp);

        // 2. Nút bấm khởi chạy
        btnStart = new JButton("🚀 Khởi chạy Server RMI");
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnStart.setBackground(new Color(40, 167, 69));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFocusPainted(false);

        JPanel topContainer = new JPanel(new BorderLayout(5, 10));
        topContainer.add(formPanel, BorderLayout.CENTER);
        topContainer.add(btnStart, BorderLayout.SOUTH);

        // 3. Khung Log hiển thị trạng thái hoạt động
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setBackground(new Color(245, 245, 245));
        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký hoạt động (System Log)"));

        mainPanel.add(topContainer, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Sự kiện khởi chạy Server
        btnStart.addActionListener(e -> startServer());
    }

    private void startServer() {
        String myIp = txtMyIp.getText().trim();
        String peerIp = txtPeerIp.getText().trim();

        if (myIp.isEmpty() || peerIp.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ IP máy này và IP Server Peer!",
                    "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnStart.setEnabled(false);
        txtMyIp.setEditable(false);
        txtPeerIp.setEditable(false);

        // Chạy RMI Registry ở luồng riêng để tránh khóa giao diện Swing
        new Thread(() -> {
            try {
                log("⏳ Đang thiết lập thuộc tính RMI hostname: " + myIp);
                System.setProperty("java.rmi.server.hostname", myIp);

                log("⏳ Đang khởi tạo RMI Registry trên Cổng 1099...");
                Registry registry = LocateRegistry.createRegistry(1099);

                SyncService service = new SyncServiceImpl(peerIp);
                registry.rebind("SyncService", service);

                log("\n=================================================");
                log("🚀 SERVER RMI ĐÃ KHỞI CHẠY THÀNH CÔNG!");
                log("👉 IP Server hiện tại: " + myIp);
                log("👉 Địa chỉ Server Peer sẵn sàng đồng bộ: " + peerIp);
                log("=================================================\n");

            } catch (Exception e) {
                log("❌ Lỗi khởi chạy Server: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    btnStart.setEnabled(true);
                    txtMyIp.setEditable(true);
                    txtPeerIp.setEditable(true);
                });
            }
        }).start();
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // Hàm tự động tìm địa chỉ IPv4 đang hoạt động của Máy thực
    private String getAutoLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ServerApp().setVisible(true));
    }
}