import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.lang.Thread;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.StringTokenizer;

class Server implements Runnable {

    private final String API = "https://api.ratesapi.io/api";

    private static final File DIR = new File("src", "resources");
    private final String httpVersion = "1.1";
    private Socket clientSocket;

    private static final OkHttpClient httpClient = new OkHttpClient();
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

            String input = in.readLine();
            StringTokenizer parse = new StringTokenizer(input);

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
            System.out.println("Calling from cache");
            return Main.cachedResponse;
        }

        String baseCurrency = (base == null) ? "USD" : base;
        String url = String.format("%s/latest?base=%s", API, baseCurrency);

        Request request = new Request.Builder().url(url).build();
        String result = "{}";
        try (Response response = httpClient.newCall(request).execute()) {
            if (! response.isSuccessful()) throw new Exception("Unexpected Code: " + response);

            result = response.body().string();
            Main.cachedResponse = result;
        } catch (Exception e) {
            System.out.println("Error getRate: " + e.getMessage());
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
