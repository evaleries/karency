import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int PORT = 2083;
    public static String cachedResponse;

    public static void main(String[] args) throws IOException {
        System.out.println("[Main] Started");
        ExecutorService executorService = Executors.newFixedThreadPool(15);

        ServerSocket server = new ServerSocket(PORT);
        while (true) {
            executorService.execute(new Server(server.accept()));
        }
    }
}
