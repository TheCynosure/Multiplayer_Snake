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
    HashMap<String, SnakeData> snakeDatas;

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
        snakeDatas = new HashMap<>();
        
        sendStartPackets();

        spawnAsyncReceiveThread();
        
        pastTime = System.currentTimeMillis();
        timeSinceUpdate = 0;
        speed = 200;

        while (connList.size() > 0) {
            long deltaTime = System.currentTimeMillis() - pastTime;
            pastTime = System.currentTimeMillis();
            timeSinceUpdate += deltaTime;
            //Added null check clause here for multiplayer stopping the move command.
            if(timeSinceUpdate > speed) {
                checkCollision();
                sendDataToClients();
            }
        }
   }
   
   private void spawnAsyncReceiveThread() {
       Thread thread = new Thread(new Runnable() {
           @Override
           public void run() {
               try {
                   socket.setSoTimeout((int) (speed / connList.size()) + 1);
               } catch (SocketException e) {
                   e.printStackTrace();
               }
               do {
                   SnakeData data = connList.get(0).receiveMessage(socket);
                   if(data != null && data.ownerAddress != null) {
                       snakeDatas.get(data.ownerAddress).vx = data.vx;
                       snakeDatas.get(data.ownerAddress).vy = data.vy;
                   }
               } while (true);
           }
       });
       thread.start();
   }

   public void checkCollision() {
       for (SnakeData data : snakeDatas.values()) {
           int headIndex = data.snake.size() - 1;
           Point head = new Point(data.snake.get(headIndex).x + data.vx, data.snake.get(headIndex).y + data.vy);
           data.snake.add(head);
           if(!head.equals(serverFoodPos)) {
               data.snake.remove(0);
           } else {
               genFoodPos();
           }

           data.GamePlaying = !(head.x < 0 || head.x > gridSize || head.y < 0 || head.y > gridSize || bodyCollision(head, data.ownerAddress));
       }
   }

   private boolean bodyCollision(Point currentSnakeHead, String headSnake) {
       for (SnakeData data : snakeDatas.values()) {
           ArrayList<Point> currentSnake = data.snake;
           for(int i = 0; i < currentSnake.size(); i++) {
               boolean isColliding = currentSnake.get(i).equals(currentSnakeHead);
               if(!data.ownerAddress.equals(headSnake) || i != currentSnake.size() - 1)
                   return isColliding;
               return false;
           }
       }
       return false;
   }

   private void sendDataToClients() {
       for(int i = 0; i < connList.size(); i++) {
           //Send every connection the data for all players
           String inet = connList.get(i).addressTo.getHostName();
           snakeDatas.get(inet).food = serverFoodPos;
           for(SnakeData snakeData: snakeDatas.values()) {
               snakeData.color = Color.RED;
               boolean beforeGamePlaying = snakeData.GamePlaying;
               snakeData.GamePlaying = snakeDatas.get(inet).GamePlaying;
               connList.get(i).sendMessage(socket, snakeData);
               snakeData.GamePlaying = beforeGamePlaying;
           }
       }
       //Remove Dead Players
       for (int i = 0; i < connList.size(); i++) {
           if(!snakeDatas.get(connList.get(i).addressTo.getHostName()).GamePlaying) {
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
           startGamePacket = new SnakeData(null, 0, 0, 0, Color.BLACK, InetAddress.getLocalHost().getHostName());
       } catch (UnknownHostException e) {
           e.printStackTrace();
       }
       for (int i = 0; i < connList.size(); i++) {
           connList.get(i).sendMessage(socket, startGamePacket);
           String ownerAddress = connList.get(i).addressTo.getHostName();
           if(ownerAddress == null)
               System.out.println("NULL OWNER ADDRESS! @StartTime");
           snakeDatas.put(ownerAddress, new SnakeData(0, 0, ownerAddress));

           snakeDatas.get(ownerAddress).snake = new ArrayList<>();
           snakeDatas.get(ownerAddress).snake.add(new Point(random.nextInt(gridSize), random.nextInt(gridSize)));
           snakeDatas.get(ownerAddress).food = serverFoodPos;
           snakeDatas.get(ownerAddress).playerAmount = connList.size();
           snakeDatas.get(ownerAddress).gridSize = gridSize;
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
