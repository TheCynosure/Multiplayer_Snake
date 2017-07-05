import com.sun.corba.se.impl.transport.ByteBufferPoolImpl;

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
    int gridSize = 40;
    java.util.List<UDP_Conn> connList = Collections.synchronizedList(new ArrayList<UDP_Conn>());
    DatagramSocket socket;
    boolean gameStarted = false;
    SnakeData[] snakeDatas;

    Random random = new Random();
    Point serverFoodPos;

    long timeSinceUpdate, speed, pastTime;

    public void genFoodPos() {
        serverFoodPos = new Point(random.nextInt(gridSize), random.nextInt(gridSize));
    }

    public static void main(String[] args) {
        try {
            Server server = new Server();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    //TODO: Client instantly dies.
    public Server() throws SocketException {
        runCommandThread();

        socket = new DatagramSocket(SERVER_PORT);
        //Set a short timeout (ms)
        socket.setSoTimeout(500);
        waitForConnections();

        //Infinite Timeout
        socket.setSoTimeout(0);
        System.out.println("Game is beginning...");
        genFoodPos();
        snakeDatas = new SnakeData[connList.size()];
        for (int i = 0; i < snakeDatas.length; i++) {
            snakeDatas[i] = new SnakeData(null, 0, 0, 0, Color.BLACK, null);
        }
        sendStartPackets();

        pastTime = System.currentTimeMillis();
        timeSinceUpdate = 0;
        speed = 200;

        while (connList.size() > 0) {
            long deltaTime = System.currentTimeMillis() - pastTime;
            pastTime = System.currentTimeMillis();
            timeSinceUpdate += deltaTime;
            //Added null check clause here for multiplayer stopping the move command.
            if(timeSinceUpdate > speed) {
                getDataFromClients();
                checkCollision();
                sendDataToClients();
            }
        }
   }

   private void getDataFromClients() {
       for(int i = 0; i < connList.size(); i++) {
           snakeDatas[i] = connList.get(i).receiveMessage(socket);
           if(snakeDatas[i].ownerAddress == null) {
               System.out.println(connList.get(i).addressTo.getHostName() + " sent me a null OwnerAddress, attempting to reassign it.");
               snakeDatas[i].ownerAddress = connList.get(i).addressTo;
           }
       }
   }

   public void checkCollision() {
       for (int i = 0; i < snakeDatas.length; i++) {
           Point head = new Point(snakeDatas[i].snake.get(snakeDatas[i].snake.size() - 1).x + snakeDatas[i].vx, snakeDatas[i].snake.get(snakeDatas[i].snake.size() - 1).y + snakeDatas[i].vy);
           snakeDatas[i].snake.add(head);
           if(!head.equals(serverFoodPos)) {
               snakeDatas[i].snake.remove(0);
           } else {
               genFoodPos();
           }

           snakeDatas[i].GamePlaying = !(head.x < 0 || head.x > gridSize || head.y < 0 || head.y > gridSize || bodyCollision(head, i));
       }
   }

   private boolean bodyCollision(Point currentSnakeHead, int snakeIndex) {
       for (int i = 0; i < snakeDatas.length; i++) {
           for (int j = 0; j < snakeDatas[i].snake.size(); j++) {
               boolean isColliding = snakeDatas[i].snake.get(j) == currentSnakeHead;
               if(i != snakeIndex || (j != snakeDatas[i].snake.size() - 1))
                    return isColliding;
               return false;
           }
       }
       return false;
   }

   private void sendDataToClients() {
       for(int i = 0; i < connList.size(); i++) {
           //Send every connection the data for all players
           snakeDatas[i].food = serverFoodPos;
           for(SnakeData snakeData: snakeDatas) {
               snakeData.color = Color.RED;
               boolean beforeGamePlaying = snakeData.GamePlaying;
               snakeData.GamePlaying = snakeDatas[i].GamePlaying;
               connList.get(i).sendMessage(socket, snakeData);
               snakeData.GamePlaying = beforeGamePlaying;
           }
       }
       //Remove Dead Players
       for (int i = 0; i < connList.size(); i++) {
           if(!snakeDatas[i].GamePlaying) {
               connList.remove(i);
               i--;
           }
       }
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
       SnakeData startGamePacket = null;
       try {
           startGamePacket = new SnakeData(null, 0, 0, 0, Color.BLACK, InetAddress.getLocalHost());
       } catch (UnknownHostException e) {
           e.printStackTrace();
       }
       for (int i = 0; i < connList.size(); i++) {
           connList.get(i).sendMessage(socket, startGamePacket);
           snakeDatas[i].snake = new ArrayList<>();
           snakeDatas[i].snake.add(new Point(random.nextInt(gridSize), random.nextInt(gridSize)));
           snakeDatas[i].food = serverFoodPos;
           snakeDatas[i].playerAmount = connList.size();
           snakeDatas[i].gridSize = gridSize;
       }
       sendDataToClients();
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
