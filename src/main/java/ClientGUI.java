import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.Naming;
import java.util.Map;

public class ClientGUI extends JFrame {
    private JTextField txtServerIp, txtKey, txtValue;
    private JButton btnConnect, btnAdd, btnUpdate, btnDelete, btnRefresh;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    private SyncService currentService;

    public ClientGUI() {
        setTitle("RMI Client - Quản lý & Đồng bộ dữ liệu");
        setSize(750, 480);
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
        txtServerIp = new JTextField("172.20.10.2", 12);
        btnConnect = new JButton("🔌 Kết nối Server");
        topPanel.add(txtServerIp);
        topPanel.add(btnConnect);

        // Center Panel: Nhập liệu & Bảng
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        formPanel.add(new JLabel("Key:"));
        txtKey = new JTextField(8);
        formPanel.add(txtKey);

        formPanel.add(new JLabel("Value:"));
        txtValue = new JTextField(10);
        formPanel.add(txtValue);

        // Tách các nút chức năng
        btnAdd = new JButton("➕ Thêm");
        btnUpdate = new JButton("✏️ Cập nhật");
        btnDelete = new JButton("❌ Xóa");
        btnRefresh = new JButton("🔄 Làm mới");

        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);
        btnRefresh.setEnabled(false);

        formPanel.add(btnAdd);
        formPanel.add(btnUpdate);
        formPanel.add(btnDelete);
        formPanel.add(btnRefresh);

        tableModel = new DefaultTableModel(new String[]{"Khóa (Key)", "Giá trị (Value)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tránh sửa trực tiếp ô trên bảng
            }
        };
        table = new JTable(tableModel);

        // Sự kiện click vào dòng trong bảng -> Tự điền Key & Value lên ô nhập
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                txtKey.setText(tableModel.getValueAt(row, 0).toString());
                txtValue.setText(tableModel.getValueAt(row, 1).toString());
            }
        });

        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Panel: Status Bar
        lblStatus = new JLabel("Chưa kết nối đến Server.");
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(lblStatus, BorderLayout.SOUTH);

        add(mainPanel);

        // Đăng ký sự kiện
        btnConnect.addActionListener(e -> connectToServer());
        btnAdd.addActionListener(e -> addData());
        btnUpdate.addActionListener(e -> updateData());
        btnDelete.addActionListener(e -> deleteData());
        btnRefresh.addActionListener(e -> refreshData());
    }

    private void connectToServer() {
        String ip = txtServerIp.getText().trim();
        try {
            String url = "rmi://" + ip + ":1099/SyncService";
            currentService = (SyncService) Naming.lookup(url);
            lblStatus.setText("✅ Đã kết nối thành công tới Server: " + ip);

            btnAdd.setEnabled(true);
            btnUpdate.setEnabled(true);
            btnDelete.setEnabled(true);
            btnRefresh.setEnabled(true);

            refreshData();
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi kết nối Server: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể kết nối Server RMI!\nKiểm tra lại IP hoặc Tường lửa.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addData() {
        String key = txtKey.getText().trim();
        String val = txtValue.getText().trim();
        if (key.isEmpty() || val.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Key và Value!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            currentService.updateData(key, val);
            txtKey.setText("");
            txtValue.setText("");
            refreshData();
            lblStatus.setText("✅ Đã thêm mới Key: " + key);
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi khi thêm dữ liệu: " + ex.getMessage());
        }
    }

    private void updateData() {
        String key = txtKey.getText().trim();
        String val = txtValue.getText().trim();
        if (key.isEmpty() || val.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng hoặc nhập Key và Value mới!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            currentService.updateData(key, val);
            txtKey.setText("");
            txtValue.setText("");
            refreshData();
            lblStatus.setText("✅ Đã cập nhật Key: " + key);
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi khi cập nhật: " + ex.getMessage());
        }
    }

    private void deleteData() {
        String key = txtKey.getText().trim();

        if (key.isEmpty() && table.getSelectedRow() != -1) {
            key = tableModel.getValueAt(table.getSelectedRow(), 0).toString();
        }

        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 dòng trên bảng hoặc nhập Key cần xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc chắn muốn xóa Key '" + key + "'?",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                currentService.deleteData(key);
                txtKey.setText("");
                txtValue.setText("");
                refreshData();
                lblStatus.setText("🗑️ Đã xóa thành công Key: " + key);
            } catch (Exception ex) {
                lblStatus.setText("❌ Lỗi khi xóa dữ liệu: " + ex.getMessage());
            }
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
            lblStatus.setText("Dữ liệu cập nhật lúc: " + java.time.LocalTime.now().toString().substring(0, 8));
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