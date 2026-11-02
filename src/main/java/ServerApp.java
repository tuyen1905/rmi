import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;

public class ServerApp extends JFrame {

    private JTextField txtMyIp;
    private JTextField txtPeerIp;
    private JButton btnDetectIp;
    private JButton btnStart;
    private JTextArea txtLog;

    private static final int RMI_PORT = 1099;
    private static final int CONNECT_TIMEOUT = 3000;

    public ServerApp() {
        setTitle("RMI Server - Trình Đồng Bộ Dữ Liệu Phân Tán");
        setSize(600, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 8));

        formPanel.add(new JLabel("IP máy này:"));
        txtMyIp = new JTextField();
        formPanel.add(txtMyIp);

        formPanel.add(new JLabel("IP Server Peer (Server còn lại):"));
        txtPeerIp = new JTextField();
        formPanel.add(txtPeerIp);

        btnDetectIp = new JButton("🔍 Tự động tìm IP");
        btnStart = new JButton("🚀 Khởi chạy Server RMI");

        btnStart.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnStart.setBackground(new Color(40, 167, 69));
        btnStart.setForeground(Color.WHITE);
        btnStart.setFocusPainted(false);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        buttonPanel.add(btnDetectIp);
        buttonPanel.add(btnStart);

        JPanel topContainer = new JPanel(new BorderLayout(5, 10));
        topContainer.add(formPanel, BorderLayout.CENTER);
        topContainer.add(buttonPanel, BorderLayout.SOUTH);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setBackground(new Color(245, 245, 245));

        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký hoạt động (System Log)"));

        mainPanel.add(topContainer, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Sự kiện nút bấm
        btnDetectIp.addActionListener(e -> detectIp());
        btnStart.addActionListener(e -> startServer());

        // Tự động quét IP máy local ngay khi mở ứng dụng
        txtMyIp.setText(getAutoLocalIp(null));
    }

    // ==========================================
    // 1. TỰ ĐỘNG SCAN IP MÁY HIỆN TẠI
    // ==========================================
    private String getAutoLocalIp(String peerIp) {
        // Cách 1: Thử dò IP tuyến đường hướng tới Peer
        if (peerIp != null && !peerIp.trim().isEmpty()) {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress peerAddress = InetAddress.getByName(peerIp.trim());
                socket.connect(peerAddress, RMI_PORT);
                InetAddress localAddress = socket.getLocalAddress();

                if (!localAddress.isAnyLocalAddress() && !localAddress.isLoopbackAddress()) {
                    return localAddress.getHostAddress();
                }
            } catch (Exception ignored) {}
        }

        // Cách 2: Quét toàn bộ Card mạng thực tế (IPv4 LAN/Wi-Fi)
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();

                if (!network.isUp() || network.isLoopback() || network.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}

        // Fallback mặc định
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "127.0.0.1";
        }
    }

    // ==========================================
    // 2. LIỆT KÊ CÁC IP CARD MẠNG VÀO LOG
    // ==========================================
    private void scanAllLocalIps() {
        log("========== DANH SÁCH IP MÁY HIỆN TẠI ==========");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                if (!network.isUp() || network.isLoopback() || network.isVirtual()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address) {
                        log("Card mạng: " + network.getDisplayName());
                        log("IP: " + address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            log("Không thể scan IP: " + e.getMessage());
        }
        log("===============================================");
    }

    // ==========================================
    // 3. XỬ LÝ NÚT TỰ ĐỘNG TÌM IP
    // ==========================================
    private void detectIp() {
        String peerIp = txtPeerIp.getText().trim();
        scanAllLocalIps();

        String myIp = getAutoLocalIp(peerIp);
        if (myIp == null || myIp.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể xác định IP mạng phù hợp!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        txtMyIp.setText(myIp);
        log("Đã xác định IP máy này: " + myIp);

        JOptionPane.showMessageDialog(
                this,
                "Đã tìm thấy IP máy hiện tại!\n\n"
                        + "IP máy này: " + myIp + "\n"
                        + "IP Peer: " + (peerIp.isEmpty() ? "(Chưa nhập)" : peerIp),
                "Scan IP thành công",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ==========================================
    // 4. KIỂM TRA KẾT NỐI TCP TỚI SERVER PEER
    // ==========================================
    private boolean checkPeerConnection(String peerIp) {
        log("⏳ Đang thử kiểm tra kết nối TCP tới Peer (" + peerIp + ":" + RMI_PORT + ")...");
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(peerIp, RMI_PORT), CONNECT_TIMEOUT);
            log("✅ Kết nối TCP tới Peer thành công!");
            return true;
        } catch (Exception e) {
            log("⚠️ Chưa thấy Peer mở Server RMI (" + e.getMessage() + ")");
            return false;
        }
    }

    // ==========================================
    // 5. KHỞI CHẠY RMI SERVER (ĐÃ SỬA LỖI LOGIC)
    // ==========================================
    private void startServer() {
        String myIp = txtMyIp.getText().trim();
        String peerIp = txtPeerIp.getText().trim();

        if (myIp.isEmpty() || peerIp.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng nhập đầy đủ IP máy này và IP Server Peer!",
                    "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            InetAddress.getByName(myIp);
            InetAddress.getByName(peerIp);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Địa chỉ IP không hợp lệ!",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        btnStart.setEnabled(false);
        btnDetectIp.setEnabled(false);
        txtMyIp.setEditable(false);
        txtPeerIp.setEditable(false);

        new Thread(() -> {
            try {
                // SỬA LỖI: Bật RMI Server Local trước, KHÔNG phụ thuộc vào Peer đã bật hay chưa
                log("⏳ Đang thiết lập RMI hostname: " + myIp);
                System.setProperty("java.rmi.server.hostname", myIp);

                log("⏳ Đang khởi tạo RMI Registry trên Cổng " + RMI_PORT + "...");
                Registry registry = LocateRegistry.createRegistry(RMI_PORT);

                SyncService service = new SyncServiceImpl(peerIp);
                registry.rebind("SyncService", service);

                log("==============================================");
                log("🚀 SERVER RMI MÁY NÀY ĐÃ KHỞI CHẠY THÀNH CÔNG!");
                log("👉 IP máy hiện tại: " + myIp);
                log("👉 Cấu hình Peer IP: " + peerIp);
                log("👉 RMI Port: " + RMI_PORT);
                log("==============================================");

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "RMI Server local đã mở thành công!\nSẵn sàng nhận kết nối từ Client và Peer.",
                            "Server hoạt động",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });

                // Kiểm tra kết nối Peer ngầm (Không làm dừng Server nếu Peer chưa bật)
                boolean connected = checkPeerConnection(peerIp);
                if (connected) {
                    log("🟢 Peer (" + peerIp + ") đang ONLINE và sẵn sàng đồng bộ.");
                } else {
                    log("🟡 Peer (" + peerIp + ") chưa bật RMI. Server này vẫn hoạt động bình thường và sẽ tự kết nối khi thực hiện đồng bộ.");
                }

            } catch (Exception e) {
                log("❌ Lỗi khởi chạy Server: " + e.getClass().getSimpleName() + " - " + e.getMessage());

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                            this,
                            "Không thể khởi chạy RMI Server!\n" + e.getMessage(),
                            "Lỗi Server",
                            JOptionPane.ERROR_MESSAGE
                    );

                    btnStart.setEnabled(true);
                    btnDetectIp.setEnabled(true);
                    txtMyIp.setEditable(true);
                    txtPeerIp.setEditable(true);
                });
            }
        }, "RMI-Server-Thread").start();
    }

    // ==========================================
    // 6. GHI LOG
    // ==========================================
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(message + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    // ==========================================
    // 7. MAIN
    // ==========================================
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ServerApp().setVisible(true));
    }
}