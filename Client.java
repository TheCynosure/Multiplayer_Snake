import java.io.IOException;
import java.net.*;

/**
 * Created by jack on 6/30/17.
 */
public class Client {
    UDP_Conn conn;
    public static void main(String[] args) {
        Client client = new Client("localhost", 5000);
    }

    public Client(String hostname, int port) {
        try {
            conn = new UDP_Conn(InetAddress.getByName(hostname), port);
            DatagramSocket socket = new DatagramSocket(4999);
            conn.sendMessage(socket, "Hello server!");
            socket.close();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
