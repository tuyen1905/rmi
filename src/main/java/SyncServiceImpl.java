import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class SyncServiceImpl extends UnicastRemoteObject implements SyncService {
    private final String peerIp;

    // --- CẤU HÌNH KẾT NỐI MYSQL ---
    // Tự động tạo DB 'rmi_db' nếu chưa có
    private static final String DB_URL = "jdbc:mysql://localhost:3306/rmi_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "1905";

    public SyncServiceImpl(String peerIp) throws RemoteException {
        super();
        this.peerIp = peerIp;
        initDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // Khởi tạo 2 bảng CSDL MySQL (dùng VARCHAR làm Primary Key)
    private void initDatabase() {
        String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                "  username VARCHAR(255) PRIMARY KEY," +
                "  password VARCHAR(255) NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        String sqlStore = "CREATE TABLE IF NOT EXISTS kv_store (" +
                "  key_name VARCHAR(255) PRIMARY KEY," +
                "  val_data TEXT NOT NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlStore);
            System.out.println("[MYSQL] Đã kết nối & khởi tạo 2 bảng CSDL (users, kv_store) thành công!");
        } catch (SQLException e) {
            System.err.println("[MYSQL ERROR] Lỗi khởi tạo CSDL: " + e.getMessage());
        }
    }

    // =============================================================
    // XỬ LÝ ĐĂNG KÝ / ĐĂNG NHẬP & ĐỒNG BỘ TÀI KHOẢN
    // =============================================================
    @Override
    public synchronized boolean registerUser(String username, String password) throws RemoteException {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("[MYSQL LOCAL] Đã tạo tài khoản mới: " + username);

            syncToPeer(peer -> peer.syncUserFromPeer(username, password), "Đăng ký tài khoản");
            return true;
        } catch (SQLException e) {
            System.err.println("[MYSQL REGISTER WARN] Tên tài khoản đã tồn tại hoặc lỗi: " + e.getMessage());
            return false;
        }
    }

    @Override
    public synchronized void syncUserFromPeer(String username, String password) throws RemoteException {
        // Cú pháp Upsert của MySQL (ON DUPLICATE KEY UPDATE)
        String sql = "INSERT INTO users(username, password) VALUES(?, ?) " +
                "ON DUPLICATE KEY UPDATE password = VALUES(password);";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("[SYNC RECEIVED] Nhận đồng bộ tài khoản từ Peer: " + username);
        } catch (SQLException e) {
            System.err.println("[SYNC USER ERROR] " + e.getMessage());
        }
    }

    @Override
    public synchronized boolean login(String username, String password) throws RemoteException {
        String sql = "SELECT password FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("password").equals(password);
            }
        } catch (SQLException e) {
            System.err.println("[LOGIN ERROR] " + e.getMessage());
        }
        return false;
    }

    // =============================================================
    // XỬ LÝ BẢNG DỮ LIỆU (THÊM / SỬA / XÓA) & ĐỒNG BỘ
    // =============================================================
    @Override
    public synchronized void saveData(String key, String value) throws RemoteException {
        executeUpsertSql(key, value);
        System.out.println("[MYSQL LOCAL] Đã lưu dữ liệu: " + key + " = " + value);

        syncToPeer(peer -> peer.syncSaveFromPeer(key, value), "Thêm/Sửa dữ liệu");
    }

    @Override
    public synchronized void syncSaveFromPeer(String key, String value) throws RemoteException {
        executeUpsertSql(key, value);
        System.out.println("[SYNC RECEIVED] Peer yêu cầu cập nhật bảng: " + key + " = " + value);
    }

    @Override
    public synchronized void deleteData(String key) throws RemoteException {
        executeDeleteSql(key);
        System.out.println("[MYSQL LOCAL] Đã xóa Key: " + key);

        syncToPeer(peer -> peer.syncDeleteFromPeer(key), "Xóa dữ liệu");
    }

    @Override
    public synchronized void syncDeleteFromPeer(String key) throws RemoteException {
        executeDeleteSql(key);
        System.out.println("[SYNC RECEIVED] Peer yêu cầu xóa Key: " + key);
    }

    @Override
    public synchronized Map<String, String> getAllData() throws RemoteException {
        Map<String, String> data = new LinkedHashMap<>();
        String sql = "SELECT key_name, val_data FROM kv_store";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                data.put(rs.getString("key_name"), rs.getString("val_data"));
            }
        } catch (SQLException e) {
            System.err.println("[MYSQL ERROR] Không thể đọc CSDL: " + e.getMessage());
        }
        return data;
    }

    // =============================================================
    // CÁC HÀM PHỤ TRỢ SQL MYSQL & ĐỒNG BỘ PEER
    // =============================================================
    private void executeUpsertSql(String key, String value) {
        String sql = "INSERT INTO kv_store(key_name, val_data) VALUES(?, ?) " +
                "ON DUPLICATE KEY UPDATE val_data = VALUES(val_data);";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MYSQL ERROR] SQL Upsert lỗi: " + e.getMessage());
        }
    }

    private void executeDeleteSql(String key) {
        String sql = "DELETE FROM kv_store WHERE key_name = ?;";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[MYSQL ERROR] SQL Delete lỗi: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface PeerTask {
        void execute(SyncService peer) throws Exception;
    }

    private void syncToPeer(PeerTask task, String actionName) {
        if (peerIp != null && !peerIp.trim().isEmpty()) {
            new Thread(() -> {
                try {
                    String url = "rmi://" + peerIp.trim() + ":1099/SyncService";
                    SyncService peerService = (SyncService) Naming.lookup(url);
                    task.execute(peerService);
                    System.out.println(" -> [SYNC SUCCESS] Đã gửi lệnh '" + actionName + "' sang Peer: " + peerIp);
                } catch (Exception e) {
                    System.err.println(" -> [SYNC WARN] Không thể đồng bộ sang Peer (" + peerIp + "): " + e.getMessage());
                }
            }).start();
        }
    }
}