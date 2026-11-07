import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface SyncService extends Remote {
    // 1. Quản lý Tài khoản (Đăng nhập / Đăng ký)
    boolean registerUser(String username, String password) throws RemoteException;
    boolean login(String username, String password) throws RemoteException;
    void syncUserFromPeer(String username, String password) throws RemoteException;

    // 2. Quản lý & Đồng bộ Bảng Dữ Liệu
    void saveData(String key, String value) throws RemoteException;
    void deleteData(String key) throws RemoteException;
    void syncSaveFromPeer(String key, String value) throws RemoteException;
    void syncDeleteFromPeer(String key) throws RemoteException;
    Map<String, String> getAllData() throws RemoteException;
}