
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SyncServiceImpl extends UnicastRemoteObject implements SyncService {
    private final Map<String, String> dataStore = new ConcurrentHashMap<>();
    private final String peerIp;

    public SyncServiceImpl(String peerIp) throws RemoteException {
        super();
        this.peerIp = peerIp;
    }

    @Override
    public synchronized void updateData(String key, String value) throws RemoteException {
        // 1. Cập nhật dữ liệu tại Server hiện tại
        dataStore.put(key, value);
        System.out.println("[LOCAL UPDATE] Key: " + key + " | Value: " + value);

        // 2. Bất đồng bộ gửi RMI sang Server Peer để đồng bộ
        notifyPeer(key, value);
    }

    @Override
    public synchronized void syncFromPeer(String key, String value) throws RemoteException {
        // Nhận dữ liệu đồng bộ từ Server kia (không gửi ngược lại để tránh vòng lặp)
        dataStore.put(key, value);
        System.out.println("[SYNC FROM PEER] Key: " + key + " | Value: " + value);
    }

    @Override
    public Map<String, String> getAllData() throws RemoteException {
        return dataStore;
    }

    private void notifyPeer(String key, String value) {
        new Thread(() -> {
            try {
                String peerUrl = "rmi://" + peerIp + ":1099/SyncService";
                SyncService peerService = (SyncService) Naming.lookup(peerUrl);
                peerService.syncFromPeer(key, value);
                System.out.println("[PEER SYNC SUCCESS] Đã đồng bộ sang " + peerIp);
            } catch (Exception e) {
                System.err.println("[PEER SYNC FAILED] Không thể kết nối Server Peer (" + peerIp + "): " + e.getMessage());
            }
        }).start();
    }
}