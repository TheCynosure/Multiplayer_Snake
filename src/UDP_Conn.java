import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

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

    public boolean sendMessage(DatagramSocket socket, SnakeData message) {
        if(socket != null) {
            try {
                byte[] snakeDataInBytes = SerializationUtils.serialize(message);
                DatagramPacket messagePacket = new DatagramPacket(snakeDataInBytes, snakeDataInBytes.length, addressTo, portTo);
                socket.send(messagePacket);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    public SnakeData receiveMessage(DatagramSocket socket) {
        SnakeData data = null;
        if(socket != null) {
            try {
                byte[] incomingData = new byte[65507];
                DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                socket.receive(incomingPacket);
                data = SerializationUtils.deserialize(incomingPacket.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return data;
    }
}
