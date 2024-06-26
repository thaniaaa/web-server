package webserver;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebServer extends Thread {
    private volatile boolean shouldStop = false;
    private ServerSocket serverSocket;
    private static String webRoot;
    private int port;
    private String logsPath;

    // Konstruktor class WebServer dengan nilai default
    public WebServer(String webRoot, String logsPath, int port) {
        this.webRoot = webRoot;
        this.logsPath = logsPath;
        this.port = port;
    }

    public static String getWebRoot() {
        return webRoot;
    }

    // untuk menghentikan servernya.
    public void stopServer() {
        try {
            shouldStop = true;
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("Server stopped.");
            } else {
                System.out.println("Server is already stopped.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    // untuk menjalankan logika utama atau alur dari webserver
    public void run() {
        try {
            // membuat socket(penghubung) menggunakan port yang ditentukan di GUI nya
            serverSocket = new ServerSocket(port);
            System.out.println("Web server started on port " + port + "...");

            // loop selama nilai shouldStop false
            while (!shouldStop) {
                Socket clientSocket = serverSocket.accept();
                // untuk menangani request dari klien
                HttpRequestHandler requestHandler = new HttpRequestHandler(clientSocket, logsPath, this);
                requestHandler.start();
            }
        } catch (IOException e) {
            if (!shouldStop) {
                // cetak stacktrace kalo misal server masih jalan pas ada kesalahan input/output
                e.printStackTrace();
            }
        } finally {
            try {
                // mastiin kalau serversocket bener bener off
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                }
            } catch (IOException e) {
                // kalau misal pas ditutup ada error, di print tuh stacktrace atau errornya.
                e.printStackTrace();
            }
        }
    }
}
