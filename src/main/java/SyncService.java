
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface SyncService extends Remote {
    // Client gọi hàm này để thêm/sửa dữ liệu
    void updateData(String key, String value) throws RemoteException;

    // Server Peer gọi hàm này để đồng bộ (tránh vòng lặp gọi lại)
    void syncFromPeer(String key, String value) throws RemoteException;

    // Lấy toàn bộ dữ liệu hiện có
    Map<String, String> getAllData() throws RemoteException;
}