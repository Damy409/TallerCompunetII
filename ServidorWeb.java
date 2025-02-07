import java.io.*;
import java.net.*;
import java.util.*;

public final class ServidorWeb {
    public static void main(String argv[]) throws Exception {
        // Establece el número de puerto.
        int puerto = 6789;
        
        // Estableciendo el socket de escucha.
        ServerSocket servidor = new ServerSocket(puerto);
        System.out.println("Servidor en el puerto " + puerto);

        // Procesando las solicitudes HTTP en un ciclo infinito.
        while (true) {
            // Escuchando las solicitudes de conexión TCP.
            Socket socket = servidor.accept();

            // Construye un objeto para procesar el mensaje de solicitud HTTP.
            SolicitudHttp solicitud = new SolicitudHttp(socket);

            // Crea un nuevo hilo para procesar la solicitud.
            Thread hilo = new Thread(solicitud);

            // Inicia el hilo.
            hilo.start();
        }
    }
}

final class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor
    public SolicitudHttp(Socket socket) throws Exception {
        this.socket = socket;
    }

    // Implementa el método run() de la interface Runnable.
    public void run() {
        try {
            proceseSolicitud();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void proceseSolicitud() throws Exception {
        // Referencia al stream de salida del socket.
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        // Referencia y filtros (InputStreamReader y BufferedReader) para el stream de entrada.
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Recoge la línea de solicitud HTTP del mensaje.
        String lineaDeSolicitud = br.readLine();

        // Muestra la línea de solicitud en la pantalla.
        System.out.println();
        System.out.println(lineaDeSolicitud);

        // Recoge y muestra las líneas de header.
        String lineaDelHeader = null;
        while ((lineaDelHeader = br.readLine()).length() != 0) {
            System.out.println(lineaDelHeader);
        }

        // Extrae el nombre del archivo de la línea de solicitud.
        StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
        partesLinea.nextToken();  // "salta" sobre el método, se supone que debe ser "GET"
        String nombreArchivo = partesLinea.nextToken();

        // Anexa un ".", de tal forma que el archivo solicitado debe estar en el directorio actual.
        nombreArchivo = "." + nombreArchivo;

        // Abre el archivo seleccionado.
        FileInputStream fis = null;
        boolean existeArchivo = true;
        try {
            fis = new FileInputStream(nombreArchivo);
        } catch (FileNotFoundException e) {
            existeArchivo = false;
        }

        // Construye el mensaje de respuesta.
        String lineaDeEstado = null;
        String lineaDeTipoContenido = null;
        String cuerpoMensaje = null;

        if (existeArchivo) {
            lineaDeEstado = "HTTP/1.1 200 OK" + CRLF;
            lineaDeTipoContenido = "Content-type: " + contentType(nombreArchivo) + CRLF;
        } else {
            lineaDeEstado = "HTTP/1.1 404 Not Found" + CRLF;
            lineaDeTipoContenido = "Content-type: text/html" + CRLF;
            cuerpoMensaje = "<HTML>" + "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" + "<BODY><b>404</b> Not Found</BODY></HTML>";
        }

        // Envia la línea de estado.
        os.writeBytes(lineaDeEstado);

        // Envía el contenido de la línea content-type.
        os.writeBytes(lineaDeTipoContenido);

        // Envía una línea en blanco para indicar el final de las líneas de header.
        os.writeBytes(CRLF);

        // Si el archivo existe, envíalo.
        if (existeArchivo) {
            byte[] buffer = new byte[1024];
            int bytesLeidos;
            while ((bytesLeidos = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesLeidos);
            }
        } else {
            // Si no existe el archivo, envía el mensaje de error.
            os.writeBytes(cuerpoMensaje);
        }

        // Cierra los streams y el socket.
        os.close();
        br.close();
    }

    // Método para determinar el tipo MIME del archivo.
    private String contentType(String nombreArchivo) {
        if (nombreArchivo.endsWith(".html")) {
            return "text/html";
        } else if (nombreArchivo.endsWith(".jpg")) {
            return "image/jpeg";
        } else if (nombreArchivo.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "application/octet-stream";
        }
    }
}
