import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Enumeration;

public class ServerApp extends JFrame {
    private JTextField txtMyIp, txtPeerIp;
    private JButton btnDetectIp, btnStart;
    private JTextArea txtLog;

    private static final int RMI_PORT = 1099;

    public ServerApp() {
        setTitle("RMI Server Node - Đồng Bộ 2 CSDL Máy Thực");
        setSize(600, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel formPanel = new JPanel(new GridLayout(2, 2, 8, 8));

        formPanel.add(new JLabel("IP Server Máy Thực Này:"));
        txtMyIp = new JTextField(getAutoLocalIp());
        formPanel.add(txtMyIp);

        formPanel.add(new JLabel("IP Server Peer (Máy Thực Còn Lại):"));
        txtPeerIp = new JTextField();
        formPanel.add(txtPeerIp);

        btnDetectIp = new JButton("🔍 Quét lại IP");
        btnStart = new JButton("🚀 Khởi chạy Server RMI & CSDL");
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnStart.setBackground(new Color(40, 167, 69));
        btnStart.setForeground(Color.WHITE);

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
        scrollPane.setBorder(BorderFactory.createTitledBorder("Nhật ký CSDL Local & Đồng Bộ Peer"));

        mainPanel.add(topContainer, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel);

        btnDetectIp.addActionListener(e -> txtMyIp.setText(getAutoLocalIp()));
        btnStart.addActionListener(e -> startServer());
    }

    private void startServer() {
        String myIp = txtMyIp.getText().trim();
        String peerIp = txtPeerIp.getText().trim();

        if (myIp.isEmpty() || peerIp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ IP máy này và IP Server Peer!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnStart.setEnabled(false);
        btnDetectIp.setEnabled(false);
        txtMyIp.setEditable(false);
        txtPeerIp.setEditable(false);

        new Thread(() -> {
            try {
                log("⏳ Thiết lập RMI hostname: " + myIp);
                System.setProperty("java.rmi.server.hostname", myIp);

                log("⏳ Khởi tạo RMI Registry tại port " + RMI_PORT + "...");
                Registry registry = LocateRegistry.createRegistry(RMI_PORT);

                SyncService service = new SyncServiceImpl(peerIp);
                registry.rebind("SyncService", service);

                log("==============================================");
                log("🚀 SERVER RMI & CSDL ĐÃ SẴN SÀNG!");
                log("👉 IP Server máy này: " + myIp);
                log("👉 IP Server Peer: " + peerIp);
                log("==============================================");

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Server RMI đã khởi chạy thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                log("❌ Lỗi khởi chạy: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi bật Server: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    btnStart.setEnabled(true);
                    btnDetectIp.setEnabled(true);
                    txtMyIp.setEditable(true);
                    txtPeerIp.setEditable(true);
                });
            }
        }, "RMI-Server-Thread").start();
    }

    private String getAutoLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new ServerApp().setVisible(true));
    }
}