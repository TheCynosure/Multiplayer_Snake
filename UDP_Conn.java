import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by jack on 7/1/17.
 */
public class UDP_Conn {
    InetAddress addressTo;
    int portTo;

    public UDP_Conn(InetAddress addressTo, int portTo) {
        this.addressTo = addressTo;
        this.portTo = portTo;
    }

    //Takes in the socket that you want to send the message through
    //And the message string you would like to send.
    //Returns true if successful and false if failed.
    public boolean sendMessage(DatagramSocket socket, byte[] message) {
        if(socket != null) {
            DatagramPacket messagePacket = new DatagramPacket(message, message.length, addressTo, portTo);
            try {
                socket.send(messagePacket);
            } catch (IOException e) {
                return false;
            }
            return true;
        }
        return false;
    }

    //Will check and return message that it finds on the given socket.
    //If there is no message or an error than a null string will be returned.
    public String receiveMessage(DatagramSocket socket) {
        String message = null;
        if(socket != null) {
            byte[] incomingData = new byte[65507];
            DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
            try {
                socket.receive(incomingPacket);
            } catch (IOException e) {}
        }
        return message;
    }
}
