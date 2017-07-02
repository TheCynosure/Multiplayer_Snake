import java.awt.*;
import java.net.*;
import java.util.ArrayList;

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

            ArrayList<Point> snake = new ArrayList<>();
            snake.add(new Point(-1, -1));

            conn.sendMessage(socket, new SnakeData(snake, 1,1,1));
            conn.sendMessage(socket, new SnakeData(snake, 0, 1, 5000));

            socket.close();
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
