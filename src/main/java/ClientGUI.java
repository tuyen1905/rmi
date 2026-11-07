import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class ClientGUI extends JFrame {
    private final SyncService currentService;
    private final String currentUser;

    private JTextField txtKey, txtValue;
    private JButton btnAdd, btnDelete, btnReload, btnLogout;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblDataStatus;

    public ClientGUI(SyncService service, String username) {
        this.currentService = service;
        this.currentUser = username;

        setTitle("RMI Client - Bảng Dữ Liệu Đồng Bộ (" + username + ")");
        setSize(750, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        reloadData();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Header Panel
        JPanel topPanel = new JPanel(new BorderLayout());
        JLabel lblHeader = new JLabel("📊 BẢNG DỮ LIỆU CSDL ĐỒNG BỘ | Xin chào: " + currentUser);
        lblHeader.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblHeader.setForeground(new Color(0, 102, 204));

        btnLogout = new JButton("🚪 Đăng xuất");
        topPanel.add(lblHeader, BorderLayout.WEST);
        topPanel.add(btnLogout, BorderLayout.EAST);

        // Center Panel: Form & Table
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));

        formPanel.add(new JLabel("Key:"));
        txtKey = new JTextField(8);
        formPanel.add(txtKey);

        formPanel.add(new JLabel("Value:"));
        txtValue = new JTextField(10);
        formPanel.add(txtValue);

        btnAdd = new JButton("➕ Thêm / Sửa");
        btnDelete = new JButton("❌ Xóa");
        btnReload = new JButton("🔄 Làm mới (Reload)");

        formPanel.add(btnAdd);
        formPanel.add(btnDelete);
        formPanel.add(btnReload);

        tableModel = new DefaultTableModel(new String[]{"Khóa (Key)", "Giá trị (Value)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);

        // Click chọn dòng trên bảng -> Fill dữ liệu lên Form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                int row = table.getSelectedRow();
                txtKey.setText(tableModel.getValueAt(row, 0).toString());
                txtValue.setText(tableModel.getValueAt(row, 1).toString());
            }
        });

        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Status Bar
        lblDataStatus = new JLabel("Sẵn sàng.");
        lblDataStatus.setBorder(BorderFactory.createEtchedBorder());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(lblDataStatus, BorderLayout.SOUTH);

        add(mainPanel);

        // Events
        btnAdd.addActionListener(e -> saveData());
        btnDelete.addActionListener(e -> deleteData());
        btnReload.addActionListener(e -> reloadData());

        // Đăng xuất -> Quay về LoginFrame
        btnLogout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });
    }

    private void saveData() {
        String key = txtKey.getText().trim();
        String val = txtValue.getText().trim();
        if (key.isEmpty() || val.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Key và Value!");
            return;
        }

        try {
            currentService.saveData(key, val);
            txtKey.setText("");
            txtValue.setText("");
            reloadData();
            lblDataStatus.setText("✅ Đã lưu vào CSDL cả 2 Server: " + key);
        } catch (Exception ex) {
            lblDataStatus.setText("❌ Lỗi lưu dữ liệu: " + ex.getMessage());
        }
    }

    private void deleteData() {
        String key = txtKey.getText().trim();
        if (key.isEmpty() && table.getSelectedRow() != -1) {
            key = tableModel.getValueAt(table.getSelectedRow(), 0).toString();
        }

        if (key.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng hoặc nhập Key cần xóa!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận xóa Key '" + key + "' trên CSDL cả 2 Server?", "Xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                currentService.deleteData(key);
                txtKey.setText("");
                txtValue.setText("");
                reloadData();
                lblDataStatus.setText("🗑️ Đã xóa Key: " + key);
            } catch (Exception ex) {
                lblDataStatus.setText("❌ Lỗi xóa dữ liệu: " + ex.getMessage());
            }
        }
    }

    private void reloadData() {
        if (currentService == null) return;
        try {
            Map<String, String> data = currentService.getAllData();
            tableModel.setRowCount(0);
            for (Map.Entry<String, String> entry : data.entrySet()) {
                tableModel.addRow(new Object[]{entry.getKey(), entry.getValue()});
            }
            lblDataStatus.setText("🔄 Đã tải lại CSDL từ Server lúc: " + java.time.LocalTime.now().toString().substring(0, 8));
        } catch (Exception ex) {
            lblDataStatus.setText("❌ Lỗi tải CSDL: " + ex.getMessage());
        }
    }
}