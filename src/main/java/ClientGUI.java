import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.Naming;
import java.util.Map;

public class ClientGUI extends JFrame {
    private JTextField txtServerIp, txtKey, txtValue;
    private JButton btnConnect, btnAdd, btnRefresh;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    private SyncService currentService;

    public ClientGUI() {
        setTitle("RMI Client - Quản lý & Đồng bộ dữ liệu");
        setSize(650, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Panel: Kết nối Server
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        topPanel.add(new JLabel("IP Server:"));
        txtServerIp = new JTextField("192.168.1.10", 12);
        btnConnect = new JButton("Kết nối Server");
        topPanel.add(txtServerIp);
        topPanel.add(btnConnect);

        // Center Panel: Nhập liệu & Bảng
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        formPanel.add(new JLabel("Key:"));
        txtKey = new JTextField(8);
        formPanel.add(txtKey);
        formPanel.add(new JLabel("Value:"));
        txtValue = new JTextField(12);
        formPanel.add(txtValue);

        btnAdd = new JButton("➕ Thêm / Cập nhật");
        btnRefresh = new JButton("🔄 Làm mới dữ liệu");
        btnAdd.setEnabled(false);
        btnRefresh.setEnabled(false);

        formPanel.add(btnAdd);
        formPanel.add(btnRefresh);

        tableModel = new DefaultTableModel(new String[]{"Khóa (Key)", "Giá trị (Value)"}, 0);
        table = new JTable(tableModel);

        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Panel: Status
        lblStatus = new JLabel("Chưa kết nối đến Server.");
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(lblStatus, BorderLayout.SOUTH);

        add(mainPanel);

        // Events
        btnConnect.addActionListener(e -> connectToServer());
        btnAdd.addActionListener(e -> addData());
        btnRefresh.addActionListener(e -> refreshData());
    }

    private void connectToServer() {
        String ip = txtServerIp.getText().trim();
        try {
            String url = "rmi://" + ip + ":1099/SyncService";
            currentService = (SyncService) Naming.lookup(url);
            lblStatus.setText("✅ Đã kết nối thành công tới Server: " + ip);
            btnAdd.setEnabled(true);
            btnRefresh.setEnabled(true);
            refreshData();
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi kết nối Server: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể kết nối Server RMI!\nCheck lại IP hoặc Firewall.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addData() {
        String key = txtKey.getText().trim();
        String val = txtValue.getText().trim();
        if (key.isEmpty() || val.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Key và Value!");
            return;
        }

        try {
            currentService.updateData(key, val);
            txtKey.setText("");
            txtValue.setText("");
            refreshData();
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi khi thêm dữ liệu: " + ex.getMessage());
        }
    }

    private void refreshData() {
        if (currentService == null) return;
        try {
            Map<String, String> data = currentService.getAllData();
            tableModel.setRowCount(0);
            for (Map.Entry<String, String> entry : data.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
            lblStatus.setText("Dữ liệu cập nhật lúc: " + java.time.LocalTime.now().toString());
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi lấy dữ liệu: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}