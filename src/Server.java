import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.*;
import java.util.*;

/**
 * Created by jack on 7/1/17.
 */
public class Server {
    final int SERVER_PORT = 5000;
    java.util.List<UDP_Conn> connList = Collections.synchronizedList(new ArrayList<UDP_Conn>());
    DatagramSocket socket;
    boolean gameStarted = false;

    public static void main(String[] args) {
        try {
            Server server = new Server();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public Server() throws SocketException {
        runCommandThread();
        socket = new DatagramSocket(SERVER_PORT);
        //Set a short timeout (ms)
        socket.setSoTimeout(500);
        waitForConnections();

        System.out.println("Game is beginning...");
        sendStartPackets();

        //TODO: Send out starting positions in the starting packets.
        //TODO: While loop receiving all the data from players.
        //TODO: Checking for collision between players and checking if they are dead.
        //TODO: Respond to players by sending them all the snake data. -- Make sure to sign it as them.
   }

   private void waitForConnections() {
       while(!gameStarted) {
           //SCAN FOR CONNECTIONS THAT SEND US A MESSAGE
           //THEN SAVE THEM FOR LATER.
           byte[] data = new byte[65507];
           DatagramPacket incomingPacket = new DatagramPacket(data, data.length);
           try {
               socket.receive(incomingPacket);
               System.out.println("ADDING: " + incomingPacket.getAddress().getHostName());
               UDP_Conn currentCon = new UDP_Conn(incomingPacket.getAddress(), incomingPacket.getPort());
               connList.add(currentCon);
           } catch (IOException e) {}
       }
   }

   private void sendStartPackets() {
       //Send packets that startGame to all players
       SnakeData startGamePacket = new SnakeData(null, 0, 0, 0);
       startGamePacket.GamePlaying = true;
       for(UDP_Conn udpConn: connList) {
           udpConn.sendMessage(socket, startGamePacket);
       }
   }

   private void runCommandThread() {
       //Thread to check for commands coming in
       Thread thread = new Thread(new Runnable() {
           @Override
           public void run() {
               Scanner scanner = new Scanner(System.in);
               while(scanner.hasNextLine()) {
                   String input = scanner.nextLine();
                   if(input.contains("/startGame"))
                       gameStarted = true;
                   else if(input.contains("/list")) {
                       System.out.println("There is " + connList.size() + " players connected!");
                       for(UDP_Conn conn: connList) {
                           System.out.println("- " + conn.addressTo.getHostName());
                       }
                   }
                   scanner.reset();
               }
               scanner.close();
           }
       });
       thread.start();
   }

   protected void finalize() throws Throwable {
       super.finalize();
       socket.close();
   }
}
