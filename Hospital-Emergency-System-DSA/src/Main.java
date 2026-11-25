import com.sun.net.httpserver.HttpServer;
import server.HospitalController;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HospitalController());
        server.setExecutor(null);
        server.start();
        System.out.println("============================================================");
        System.out.println(" MedCare Emergency Triage & Patient Management Started!");
        System.out.println(" Open in Browser: http://localhost:" + port);
        System.out.println("============================================================");
    }
}
