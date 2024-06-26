/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package webserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author thania aprilah
 */
//menangani permintaan GET, melayani file, menampilkan daftar direktori, dan mencatat permintaan ke dalam log.


public class HttpRequestHandler extends Thread {
        private Socket socket; //socket buat koneksi ke klien
        private String logsPath; //path ke direktori logs yang disetel sesuai dengan GUI nya
        private WebServer webServer;
        

        //konstruktor untuk httpsrequesthandlernya
        public HttpRequestHandler(Socket socket, String logsPath, WebServer server) {
            this.socket = socket;
            this.logsPath = logsPath;
            this.webServer = server;
            
        }

        
        @Override
        //ini menangani logika atau alur dari httprequesthandler
        public void run() {
            try {
                //membaca input seperti GET /index.html HTTP/1.1
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                //menulis outputnya
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());


                String requestLine = in.readLine(); //membaca baris permintaan dari klien
                String[] tokens = requestLine.split(" ");//dipecah permintaannya jadi beberapa token
                String method = tokens[0];//ndapetin metode httpnya (biasanya get)
                String requestURL = tokens[1];//ndapetin urlnya

                // ngecek metodenya, kalau get ya diproses nantinya
                if (method.equals("GET")) {
                    serveFile(requestURL, out);
                } else {
                    //metode selain get bakal di respon not implemented
                    String response = "HTTP/1.1 501 Not Implemented\r\n\r\n";
                    out.writeBytes(response);
                }
                //nyatet di lognya.
                logAccess(requestURL, socket.getInetAddress().getHostAddress(), requestURL);

                //close / nutup input output socketnya.
                out.close();
                in.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
              
        //metode buat ngehandle file yang direquest klien
        private void serveFile(String requestURL, DataOutputStream out) throws IOException {
            try {
                //ndapetin path filenya
                String filePath =  WebServer.getWebRoot() + requestURL.replace("/", "\\");
                //mbuat objeknya
                File file = new File(filePath);

                
                if (file.exists()) {
                    if (file.isDirectory()) {
                        if (requestURL.endsWith("/")) {
                            // melayani daftar direktori kalau filenya akhir akhirannya "/"
                            listDirectory(file, out, getParentDirectory(requestURL));
                        } else {
                            // mengarahkan ke URL dengan akhiran "/" (kalo gaada / nanti dikasih /)
                            String redirectURL = requestURL + "/";
                            String response = "HTTP/1.1 301 Moved Permanently\r\nLocation: " + redirectURL + "\r\n\r\n";
                            out.writeBytes(response);
                        }
                    } else {
                        // Menentukan jenis konten berdasarkan ekstensi file
                        String contentType = getContentType(file);

                        // Membaca konten file dan mengirimkannya sebagai respons
                        byte[] fileData = Files.readAllBytes(file.toPath());
                        String response = "HTTP/1.1 200 OK\r\nContent-Length: " + fileData.length +
                                "\r\nContent-Type: " + contentType + "\r\n\r\n";
                        out.writeBytes(response);
                        out.write(fileData);
                    }
                } else {
                    // Kalau File not Found
                    String response = "HTTP/1.1 404 Not Found\r\n\r\n";
                    out.writeBytes(response);
                }
            } catch (IOException e) {
                //kalau error di catch errornya apa
                String errorMessage = e.getMessage();
                String response = "HTTP/1.1 500 Internal Server Error\r\n\r\n";
                out.writeBytes(response);
                
                //kalaupun tidak error, nanti akan tetap ditulis logs nya.
//                logAccess(requestURL, socket.getInetAddress().getHostAddress(), errorMessage);
            }
        }

        //ngambil type file nya
        private String getContentType(File file) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                return "text/html";
            } else if (fileName.endsWith(".pdf")) {
                return "application/pdf";
            } else if (fileName.endsWith(".docx")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (fileName.endsWith(".txt")) {
                return "text/plain";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".gif")) {
                return "image/gif";
            } else if (fileName.endsWith(".css")) {
                 return "text/css";
            } else {
                // Default content type for unknown file types
                return "application/octet-stream";
            }
        }

        
        private void listDirectory(File directory, DataOutputStream out, String parentDirectory) throws IOException {
            // Mendapatkan daftar file dalam direktori yang diberikan
            File[] files = directory.listFiles();
             // Membangun respons HTML untuk menampilkan daftar file
            StringBuilder responseBuilder = new StringBuilder("<html><body><h1>Directory Listing</h1>");

            // Menambahkan tombol "Back" jika tidak berada di direktori root
            if (parentDirectory != null) {
                responseBuilder.append("<button onclick=\"goBack()\">Back</button><br>");
            }

            // mbuat daftar file yang ada dalam suatu direktori dalam bentuk list dan hyperlink
          /*  responseBuilder.append("<ul>");
            for (File file : files) {
                String fileName = file.getName();
                //untuk memberi hyperlink kepada setiap file yang ada di dalam list
                responseBuilder.append("<li><a href=\"").append(fileName).append("\">").append(fileName).append("</a></li>");
            } */
          
        /* responseBuilder.append("<table border=\"1\">");
         responseBuilder.append("<tr><th>Icon</th><th>Directory List</th><th>Size</th></tr>");

         for (File file : files) {
        String fileName = file.getName();
        String icon = file.isDirectory() ? "folder-icon.png" : "file-icon.png"; // Ganti dengan path icon yang sesuai
        long fileSize = file.length(); // Mendapatkan ukuran file, untuk direktori akan mengembalikan 0
    
        responseBuilder.append("<tr>");
        responseBuilder.append("<td><img src=\"").append(icon).append("\" alt=\"").append(file.isDirectory() ? "Folder" : "File").append("\" /></td>");
        responseBuilder.append("<td><a href=\"").append(fileName).append("\">").append(fileName).append("</a></td>");
        responseBuilder.append("<td>").append(fileSize).append(" bytes</td>");
        responseBuilder.append("</tr>");
        }

        responseBuilder.append("</table>"); */
        
        /*responseBuilder.append("Icon\tName\tSize\n");

for (File file : files) {
    String fileName = file.getName();
    // Menggunakan simbol Unicode untuk folder dan file
    String iconSymbol = file.isDirectory() ? "\uD83D\uDCC1" : "\uD83D\uDCC4"; // 📁 untuk folder dan 📄 untuk file
    long fileSize = file.length(); // Mendapatkan ukuran file, untuk direktori akan mengembalikan 0
    
    responseBuilder.append(iconSymbol).append("\t").append(fileName).append("\t").append(fileSize).append(" bytes\n");
}
*/
        
    responseBuilder.append("<table border=\"1\">");
responseBuilder.append("<tr><th>Icon</th><th>Name</th><th>Size</th></tr>");

for (File file : files) {
    String fileName = file.getName();
    // Menggunakan URL ikon dari layanan CDN atau sumber lain
    String iconUrl = file.isDirectory() 
        ? "https://cdn-icons-png.flaticon.com/512/716/716784.png"  // Ikon folder
        : "https://cdn-icons-png.flaticon.com/512/716/716785.png"; // Ikon file
    long fileSize = file.length(); // Mendapatkan ukuran file, untuk direktori akan mengembalikan 0
    
    responseBuilder.append("<tr>");
    responseBuilder.append("<td><img src=\"").append(iconUrl).append("\" alt=\"").append(file.isDirectory() ? "Folder" : "File").append("\" width=\"24\" height=\"24\"/></td>");
    responseBuilder.append("<td><a href=\"").append(fileName).append("\">").append(fileName).append("</a></td>");
    responseBuilder.append("<td>").append(fileSize).append(" bytes</td>");
    responseBuilder.append("</tr>");
}

responseBuilder.append("</table>");







            //supaya tombol backnya bisa digunakan (javascript)
            responseBuilder.append("</ul>");
            responseBuilder.append("<script>");
            responseBuilder.append("function goBack() { window.history.back(); }"); // Script JavaScript untuk kembali
            responseBuilder.append("</script>");
            responseBuilder.append("</body></html>");

            //untuk merespon permintaan klien saat merequest suatu file untuk dibuka
            String response = "HTTP/1.1 200 OK\r\nContent-Length: " + responseBuilder.length() +
                    "\r\nContent-Type: text/html\r\n\r\n" + responseBuilder.toString();
            //untuk menampilkan request file yang mau dibuka tadi (menampilkan isi konten file dalam bentuk htlm)
            out.writeBytes(response);
        }


        private String getParentDirectory(String requestURL) {
            // Mencari indeks posisi terakhir tanda '/' dalam URL permintaan
            int lastSlashIndex = requestURL.lastIndexOf("/");
            // Memeriksa apakah URL bukan merupakan root directory
            if (lastSlashIndex > 0) {
                 // Mengembalikan substring URL dari awal hingga sebelum tanda '/'
                return requestURL.substring(0, lastSlashIndex);
            }
            // Jika URL adalah root directory, mengembalikan null
            return null;
        }
        
        //digunakan untuk membuat logs
        private void logAccess(String requestURL, String ipAddress, String errorMessage) {
            //Tanggal Saat ini  dalam format yyyy-mm-dd
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            // Mendapatkan tanggal saat ini dalam format yang ditentukan
            String logFileName = dateFormat.format(new Date()) + ".log";
            // Menggabungkan path dari direktori logs dengan nama file log
            String logFilePath = Paths.get(logsPath, logFileName).toString();
            try {
                //membuat objek logsdir
                File logsDir = new File(logsPath);
                if (!logsDir.exists()) {
                    //membuat direktori logs kalau semisal belum ada direktori
                    logsDir.createNewFile();
                }
                //membuat objek file
                File logFile = new File(logFilePath);
                if (!logFile.exists()) {
                    //Membuat FileLog kalau pada direktori tersebut belum ada file log
                    logFile.createNewFile();
                }
                 // Format pesan log dengan tanggal, alamat IP, dan URL permintaan
                String logEntry = String.format("[%s] %s - %s\n", new Date(), ipAddress, requestURL);
                //Tulis log entry ke dalam file log, di append biar log sebelumnya tidak terhapus
                Files.write(Paths.get(logFilePath), logEntry.getBytes(), java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
