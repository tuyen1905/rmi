import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.rmi.Naming;
import java.util.Map;

public class ClientGUI extends JFrame {
    private JTextField txtServerIp, txtKey, txtValue;
    private JButton btnConnect, btnAdd, btnUpdate, btnDelete;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblStatus;

    private SyncService currentService;

    public ClientGUI() {
        setTitle("RMI Client - Quản lý & Đồng bộ dữ liệu");
        setSize(720, 480);
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

        // Khai báo 3 nút chức năng rõ ràng
        btnAdd = new JButton("➕ Thêm dữ liệu");
        btnUpdate = new JButton("🔄 Cập nhật (Reload)");
        btnDelete = new JButton("❌ Xóa dữ liệu");

        btnAdd.setEnabled(false);
        btnUpdate.setEnabled(false);
        btnDelete.setEnabled(false);

        formPanel.add(btnAdd);
        formPanel.add(btnUpdate);
        formPanel.add(btnDelete);

        tableModel = new DefaultTableModel(new String[]{"Khóa (Key)", "Giá trị (Value)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Không cho sửa trực tiếp trên ô của bảng
            }
        };
        table = new JTable(tableModel);

        // Click chọn dòng trên bảng -> Tự điền Key & Value xuống ô nhập
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                txtKey.setText(tableModel.getValueAt(row, 0).toString());
                txtValue.setText(tableModel.getValueAt(row, 1).toString());
            }
        });

        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom Panel: Thanh trạng thái (Status Bar)
        lblStatus = new JLabel("Chưa kết nối đến Server.");
        lblStatus.setBorder(BorderFactory.createEtchedBorder());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(lblStatus, BorderLayout.SOUTH);

        add(mainPanel);

        // Đăng ký sự kiện nút bấm
        btnConnect.addActionListener(e -> connectToServer());
        btnAdd.addActionListener(e -> addData());
        btnUpdate.addActionListener(e -> reloadData()); // Gọi hàm Reload dữ liệu từ Server
        btnDelete.addActionListener(e -> deleteData());
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

            reloadData(); // Tải dữ liệu ngay khi kết nối thành công
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi kết nối Server: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Không thể kết nối Server RMI!\nKiểm tra lại IP hoặc Tường lửa.", "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 1. CHỨC NĂNG THÊM: Gửi Key/Value mới lên Server
    private void addData() {
        String key = txtKey.getText().trim();
        String val = txtValue.getText().trim();
        if (key.isEmpty() || val.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Key và Value để thêm!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            currentService.updateData(key, val);
            txtKey.setText("");
            txtValue.setText("");
            reloadData(); // Thêm xong tự động làm mới bảng
            lblStatus.setText("✅ Đã thêm mới Key: " + key);
        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi khi thêm dữ liệu: " + ex.getMessage());
        }
    }

    // 2. CHỨC NĂNG CẬP NHẬT (RELOAD): Chỉ kéo dữ liệu mới nhất từ Server về hiển thị
    private void reloadData() {
        if (currentService == null) return;
        try {
            Map<String, String> data = currentService.getAllData();
            tableModel.setRowCount(0); // Xóa bảng cũ

            // Đổ lại toàn bộ dữ liệu mới từ Server (bao gồm dữ liệu từ máy Client khác)
            for (Map.Entry<String, String> entry : data.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }

            String currentTime = java.time.LocalTime.now().toString();
            if (currentTime.contains(".")) {
                currentTime = currentTime.substring(0, currentTime.indexOf("."));
            }
            lblStatus.setText("🔄 Đã cập nhật lại dữ liệu từ Server lúc: " + currentTime);

        } catch (Exception ex) {
            lblStatus.setText("❌ Lỗi tải dữ liệu: " + ex.getMessage());
        }
    }

    // 3. CHỨC NĂNG XÓA: Xóa bản ghi đã chọn
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
                reloadData(); // Xóa xong tự động cập nhật lại bảng
                lblStatus.setText("🗑️ Đã xóa thành công Key: " + key);
            } catch (Exception ex) {
                lblStatus.setText("❌ Lỗi khi xóa dữ liệu: " + ex.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ClientGUI().setVisible(true));
    }
}