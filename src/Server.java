import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

class Server implements Runnable {

    private final String API = "https://api.ratesapi.io/api";

    private static final File DIR = new File("src", "resources");
    private final String httpVersion = "1.1";
    private Socket clientSocket;

    private BufferedOutputStream bufOut;
    private BufferedReader in;
    private PrintWriter out;
    private String requestFile;
    private String requestMethod;

    public Server(Socket client) {
        clientSocket = client;
    }

    @Override
    public void run() {

        try {
            System.out.println("Thread: " + Thread.currentThread().getName());

            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream());
            bufOut = new BufferedOutputStream(clientSocket.getOutputStream());

            String input = in.readLine(); // GET / HTTP/1.1
            if (input == null) {
                throw new Exception("Error input, null");
            }

            StringTokenizer parse = new StringTokenizer(input); // ["GET", "/", "HTTP/1.1"]

            System.out.println(input);

            requestMethod = parse.nextToken().toUpperCase();
            requestFile = parse.nextToken().toLowerCase();

            HashMap<String, String> headers = new HashMap<>();
            headers.put("Date", new Date().toString());
            if (requestMethod.equals("GET") && requestFile.endsWith("/")) {
                File file = new File(DIR, "index.html");
                int contentLength = (int) file.length();
                byte[] body = readFileData(file, contentLength);
                headers.put("Content-Type",  "text/html");
                headers.put("Content-Length", String.valueOf(contentLength));
                headers.put("Connection", "keep-alive");
                sendHeaders("200 OK", headers);

                bufOut.write(body, 0, contentLength);
                bufOut.flush();
            } else if (requestMethod.equals("GET") && requestFile.startsWith("/rates")) {
                System.out.println("----------------------------> Request ke Rate");
                String responseAPI = getRate("USD");
                headers.put("Content-Type", "application/json");
                headers.put("Content-Length", String.valueOf(responseAPI.length()));
                headers.put("Connection", "close");
                headers.put("Cache-Control", "no-cache, no-store, must-revalidate");
                headers.put("Pragma", "no-cache");
                sendHeaders("200 OK", headers);

                bufOut.write(responseAPI.getBytes(), 0, responseAPI.length());
                bufOut.flush();
            } else {
                System.out.println("Unfiltered: " + requestMethod + " " + requestFile);
                throw new FileNotFoundException();
            }

        } catch (FileNotFoundException e) {
            System.out.println("File tidak ditemukan: " + requestFile + " -> " + new File(DIR, requestFile).getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRate(String base) throws IOException {

        if (Main.cachedResponse != null) {
            System.out.println("Getting response from cache");
            System.out.println(Main.cachedResponse);
            return Main.cachedResponse;
        }

        String host = "100.25.153.103";

        String result = "{}";
        try {
            Socket socket = new Socket(host, 80);

            BufferedReader inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter outStream = new PrintWriter(socket.getOutputStream(), true);

            outStream.println("GET /api.php HTTP/1.1");
            outStream.println("Host: " + host);
            outStream.println("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:77.0) Gecko/20100101 Firefox/77.0");
            outStream.println("Connection: close");
            outStream.println();

            String headerRes = "";
            while ((headerRes = inStream.readLine()) != null) {
                if (headerRes.startsWith("{") && headerRes.endsWith("}")) {
                    result = headerRes;
                    System.out.println("Response dari Server: ");
                }
                System.out.println(headerRes);
            }

            Main.cachedResponse = result;
        } catch (Exception e) {
            System.out.println("Error saat memanggil API: ");
            e.printStackTrace();
        }

        return result;
    }

    private byte[] readFileData(File file, int length) throws IOException {
        FileInputStream fileInputStream = null;
        byte[] content = new byte[length];

        try {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(content);
        } finally {
            if (fileInputStream != null)
                fileInputStream.close();
        }

        return content;
    }

    private void sendHeaders(String httpStatus, HashMap<String, String> headers) {

        out.println(String.format("HTTP/%s %s", httpVersion, httpStatus));

        if (headers != null) {
            headers.forEach((key, value) -> {
                out.println(String.format("%s: %s", key, value));
            });
            out.println();
        }

        out.flush();

    }

}
