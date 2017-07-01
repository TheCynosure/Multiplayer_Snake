import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Created by jack on 7/1/17.
 */
public class Server_Test_Connections {
    final int SERVER_PORT = 5000;
    ArrayList<UDP_Conn> connList = new ArrayList<>();

    public static void main(String[] args) {
        try {
            Server_Test_Connections server_test_connections = new Server_Test_Connections();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public Server_Test_Connections() throws SocketException {
        DatagramSocket socket = new DatagramSocket(SERVER_PORT);
        //Set a short timeout (ms)
        socket.setSoTimeout(500);
        while(true) {
            byte[] data = new byte[65507];
            DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
            try {
                socket.receive(incomingPacket);
                System.out.println("MSG FROM: " + incomingPacket.getAddress() + " MSG: " + new String(incomingPacket.getData(), 0, incomingPacket.getLength()));
                System.out.println("ADDING " + incomingPacket.getAddress() + " TO THE CONN LIST");
                connList.add(new UDP_Conn(incomingPacket.getAddress(), incomingPacket.getPort()));
            } catch (SocketTimeoutException f) {} catch (IOException e) {
                e.printStackTrace();
            }
        }
   }
}
