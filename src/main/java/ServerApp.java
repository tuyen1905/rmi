import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class ServerApp {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);

            System.out.print("Nhập IP của MÁY THỰC NÀY: ");
            String myIp = scanner.nextLine().trim();

            System.out.print("Nhập IP của MÁY THỰC PEER (Server còn lại): ");
            String peerIp = scanner.nextLine().trim();

            // CẤU HÌNH BẮT BUỘC: Đăng ký IP máy thực để Client trên máy ảo tìm đúng địa chỉ
            System.setProperty("java.rmi.server.hostname", myIp);

            // Tạo RMI Registry trên Port 1099
            Registry registry = LocateRegistry.createRegistry(1099);

            // Khởi tạo và Bind dịch vụ
            SyncService service = new SyncServiceImpl(peerIp);
            registry.rebind("SyncService", service);

            System.out.println("\n=================================================");
            System.out.println("🚀 SERVER RMI ĐÃ KHỞI CHẠY THÀNH CÔNG!");
            System.out.println("👉 IP Server hiện tại: " + myIp);
            System.out.println("👉 Đang kết nối đồng bộ với Peer IP: " + peerIp);
            System.out.println("=================================================");

        } catch (Exception e) {
            System.err.println("Lỗi khởi chạy Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}