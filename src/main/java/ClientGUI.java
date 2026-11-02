import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.rmi.Naming;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientGUI extends JFrame {
    private JTextField txtServerIp, txtUsername, txtMessage;
    private JButton btnConnect, btnSend, btnReload;
    private JPanel chatContainerPanel; // Khung chứa các bóng tin nhắn Zalo
    private JScrollPane scrollPane;
    private JLabel lblStatus;

    private SyncService currentService;
    private Timer autoRefreshTimer;

    // Màu sắc chủ đạo chuẩn phong cách Zalo
    private static final Color ZALO_BLUE = new Color(0, 104, 255);
    private static final Color ZALO_BG = new Color(234, 238, 243);
    private static final Color MY_BUBBLE_BG = new Color(0, 104, 255);
    private static final Color PEER_BUBBLE_BG = Color.WHITE;

    public ClientGUI() {
        setTitle("Zalo RMI Chat - Phòng Chat Phân Tán");
        setSize(750, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ZALO_BG);

        // ==========================================
        // 1. HEADER (Thanh trên cùng dạng Zalo)
        // ==========================================
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        headerPanel.setBackground(ZALO_BLUE);

        JLabel lblTitle = new JLabel("💬 Zalo Chat RMI ");
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitle.setForeground(Color.WHITE);
        headerPanel.add(lblTitle);

        JLabel lblIp = new JLabel("IP Server:");
        lblIp.setForeground(Color.WHITE);
        headerPanel.add(lblIp);

        txtServerIp = new JTextField("172.20.10.2", 10);
        headerPanel.add(txtServerIp);

        JLabel lblUser = new JLabel("Tên bạn:");
        lblUser.setForeground(Color.WHITE);
        headerPanel.add(lblUser);

        txtUsername = new JTextField("User_" + (int)(Math.random() * 900 + 100), 7);
        headerPanel.add(txtUsername);

        btnConnect = new JButton("🔌 Kết nối");
        btnConnect.setBackground(Color.WHITE);
        btnConnect.setForeground(ZALO_BLUE);
        btnConnect.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnConnect.setFocusPainted(false);
        headerPanel.add(btnConnect);

        // ==========================================
        // 2. KHUNG TRO CHUYỆN (Chat Container)
        // ==========================================
        chatContainerPanel = new JPanel();
        chatContainerPanel.setLayout(new BoxLayout(chatContainerPanel, BoxLayout.Y_AXIS));
        chatContainerPanel.setBackground(ZALO_BG);

        scrollPane = new JScrollPane(chatContainerPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // ==========================================
        // 3. KHUNG NHẬP TIN NHẮN (Bottom Input Bar)
        // ==========================================
        JPanel southContainer = new JPanel(new BorderLayout(5, 5));
        southContainer.setBackground(Color.WHITE);
        southContainer.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        txtMessage = new JTextField();
        txtMessage.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtMessage.setEnabled(false);
        txtMessage.setMargin(new Insets(5, 8, 5, 8));

        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonGroup.setBackground(Color.WHITE);

        btnSend = new JButton("Gửi 📤");
        btnSend.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnSend.setBackground(ZALO_BLUE);
        btnSend.setForeground(Color.WHITE);
        btnSend.setEnabled(false);
        btnSend.setFocusPainted(false);

        btnReload = new JButton("🔄");
        btnReload.setToolTipText("Cập nhật lại tin nhắn");
        btnReload.setEnabled(false);

        buttonGroup.add(btnReload);
        buttonGroup.add(btnSend);

        southContainer.add(txtMessage, BorderLayout.CENTER);
        southContainer.add(buttonGroup, BorderLayout.EAST);

        // Thanh trạng thái (Status Bar)
        lblStatus = new JLabel(" Chưa kết nối đến Server Chat.");
        lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblStatus.setForeground(Color.GRAY);
        southContainer.add(lblStatus, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(southContainer, BorderLayout.SOUTH);

        add(mainPanel);

        // Sự kiện
        btnConnect.addActionListener(e -> connectToServer());
        btnSend.addActionListener(e -> sendMessage());
        btnReload.addActionListener(e -> reloadChat());

        txtMessage.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        // Tự động làm mới tin nhắn mỗi 1.5 giây
        autoRefreshTimer = new Timer(1500, e -> reloadChat());
    }

    private void connectToServer() {
        String ip = txtServerIp.getText().trim();
        String username = txtUsername.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên người dùng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String url = "rmi://" + ip + ":1099/SyncService";
            currentService = (SyncService) Naming.lookup(url);

            lblStatus.setText(" ✅ Đã kết nối tới Server Chat: " + ip);

            txtServerIp.setEditable(false);
            txtUsername.setEditable(false);
            btnConnect.setEnabled(false);

            txtMessage.setEnabled(true);
            btnSend.setEnabled(true);
            btnReload.setEnabled(true);

            reloadChat();
            autoRefreshTimer.start();

        } catch (Exception ex) {
            lblStatus.setText(" ❌ Lỗi kết nối Server: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể kết nối Server RMI Chat!\nKiểm tra IP hoặc Tường lửa.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String username = txtUsername.getText().trim();
        String msg = txtMessage.getText().trim();

        if (msg.isEmpty()) return;

        try {
            currentService.sendMessage(username, msg);
            txtMessage.setText("");
            reloadChat();
        } catch (Exception ex) {
            lblStatus.setText(" ❌ Lỗi khi gửi tin nhắn: " + ex.getMessage());
        }
    }

    // Tải và hiển thị tin nhắn dưới dạng Bong Bóng Chat Zalo
    private void reloadChat() {
        if (currentService == null) return;
        try {
            List<String> chatHistory = currentService.getChatHistory();
            String myUser = txtUsername.getText().trim();

            chatContainerPanel.removeAll(); // Xóa UI cũ để vẽ lại

            // Định dạng chuỗi tin nhắn: "[HH:mm:ss] Username: Content"
            Pattern pattern = Pattern.compile("^\\[(.*?)\\] (.*?): (.*)$");

            for (String rawLine : chatHistory) {
                Matcher matcher = pattern.matcher(rawLine);
                if (matcher.find()) {
                    String time = matcher.group(1);
                    String sender = matcher.group(2);
                    String content = matcher.group(3);

                    boolean isMe = sender.equalsIgnoreCase(myUser);
                    chatContainerPanel.add(createZaloBubble(sender, content, time, isMe));
                } else {
                    // Dòng không đúng chuẩn định dạng
                    chatContainerPanel.add(createSystemNotice(rawLine));
                }
            }

            chatContainerPanel.revalidate();
            chatContainerPanel.repaint();

            // Tự động cuộn xuống đáy
            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = scrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

            lblStatus.setText(" 💬 Trực tuyến | Đội hình Chat Zalo sẵn sàng");

        } catch (Exception ex) {
            lblStatus.setText(" ❌ Lỗi tải tin nhắn: " + ex.getMessage());
        }
    }

    // ==========================================
    // CẤU TRÚC BONG BÓNG CHAT KIỂU ZALO (LEFT / RIGHT)
    // ==========================================
    private JPanel createZaloBubble(String sender, String content, String time, boolean isMe) {
        // Wrapper Panel định vị Bên Trái (Peer) hoặc Bên Phải (Me)
        JPanel wrapper = new JPanel(new FlowLayout(isMe ? FlowLayout.RIGHT : FlowLayout.LEFT, 12, 4));
        wrapper.setOpaque(false);

        // Bong bóng chứa nội dung
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBackground(isMe ? MY_BUBBLE_BG : PEER_BUBBLE_BG);
        bubble.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isMe ? MY_BUBBLE_BG : new Color(220, 224, 230), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        // 1. Tên người gửi (Chỉ hiển thị nếu là người khác gửi)
        if (!isMe) {
            JLabel lblSender = new JLabel(sender);
            lblSender.setFont(new Font("SansSerif", Font.BOLD, 11));
            lblSender.setForeground(new Color(0, 100, 200));
            bubble.add(lblSender);
            bubble.add(Box.createVerticalStrut(2));
        }

        // 2. Nội dung tin nhắn
        JLabel lblContent = new JLabel("<html><body style='width: 220px; word-wrap: break-word;'>" + escapeHtml(content) + "</body></html>");
        lblContent.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lblContent.setForeground(isMe ? Color.WHITE : Color.BLACK);
        bubble.add(lblContent);

        // 3. Giờ gửi tin nhắn nằm ở dưới khung bóng
        JLabel lblTime = new JLabel(time);
        lblTime.setFont(new Font("SansSerif", Font.PLAIN, 9));
        lblTime.setForeground(isMe ? new Color(220, 235, 255) : Color.GRAY);
        lblTime.setAlignmentX(Component.RIGHT_ALIGNMENT);
        bubble.add(Box.createVerticalStrut(2));
        bubble.add(lblTime);

        wrapper.add(bubble);
        return wrapper;
    }

    private JPanel createSystemNotice(String msg) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        wrapper.setOpaque(false);
        JLabel lbl = new JLabel(msg);
        lbl.setFont(new Font("SansSerif", Font.ITALIC, 10));
        lbl.setForeground(Color.GRAY);
        wrapper.add(lbl);
        return wrapper;
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}