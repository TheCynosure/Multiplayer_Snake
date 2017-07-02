import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by jack on 6/30/17.
 */
public class Snake extends JFrame implements KeyListener {
    int pixelSize = 25;
    int gridSize = 40;
    int vx, vy;
    Random random;
    ArrayList<Point> snake;
    Point food;
    long pastTime, timeSinceUpdate, speed;

    public static void main(String[] args) {
        //Turned into JFrame and added these!
        Snake snake = new Snake();
        snake.init();
    }

    public void init() {
        //Enable Multiplayer.
        online = true;

        setSize(gridSize * pixelSize, gridSize * pixelSize);
        setFocusable(true);
        addKeyListener(this);
        snake = new ArrayList<>();
        snake.add(new Point(11,10));
        snake.add(new Point(12, 10));
        snake.add(new Point(13, 10));
        vx = 1;
        vy = 0;
        random = new Random();
        pastTime = System.currentTimeMillis();
        timeSinceUpdate = 0;
        speed = 50;

        if(online) {
            establishConnection();
            System.out.println("Waiting for Multiplayer Game");
            waitForGameStart();
            System.out.println("Game is beginning...");
        }

        setVisible(true);
        spawnFood();
        refresh();
    }

    @Override
    public void paint(Graphics g) {
        BufferedImage bufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics h = bufferedImage.getGraphics();
        h.setColor(Color.WHITE);
        h.fillRect(0,0,getWidth(),getHeight());
        h.setColor(Color.ORANGE);
        h.fillOval(food.x * pixelSize, food.y * pixelSize, pixelSize, pixelSize);
        if(online) {
            for(SnakeData snakeData: playerData) {
                h.setColor(snakeData.color);
                for (Point p : snakeData.snake) {
                    h.fillRect(p.x * pixelSize, p.y * pixelSize, pixelSize, pixelSize);
                }
            }
        } else {
            h.setColor(Color.GREEN);
            for (Point p : snake) {
                h.fillRect(p.x * pixelSize, p.y * pixelSize, pixelSize, pixelSize);
            }
        }
        g.drawImage(bufferedImage, 0, 0, null);
        refresh();
    }

    public Point getHead() {
        return snake.get(snake.size() - 1);
    }

    public void move() {
        Point head = getHead();
        snake.add(new Point(head.x + vx, head.y + vy));

        if(getHead().equals(food)) {
            spawnFood();
        } else {
            snake.remove(0);
        }

        if(getHead().x < 0 || getHead().x > gridSize || getHead().y < 0 || getHead().y > gridSize) {
            //YOU LOSE
            init();
        }

        for(int i = 0; i < snake.size() - 1; i++) {
            if(getHead().equals(snake.get(i))) {
                init();
            }
        }
    }

    public void spawnFood() {
        food = new Point(random.nextInt(gridSize), random.nextInt(gridSize));
    }

    public void refresh() {
        long deltaTime = System.currentTimeMillis() - pastTime;
        pastTime = System.currentTimeMillis();
        timeSinceUpdate += deltaTime;
        //Added null check clause here for multiplayer stopping the move command.
        if(timeSinceUpdate > speed && !online) {
            move();
            timeSinceUpdate -= speed;
        } else if(online) {
            sendData();
            processData();
        }
        repaint();
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_UP:
                vy = -1;
                vx = 0;
                break;
            case KeyEvent.VK_DOWN:
                vy = 1;
                vx = 0;
                break;
            case KeyEvent.VK_LEFT:
                vy = 0;
                vx = -1;
                break;
            case KeyEvent.VK_RIGHT:
                vy = 0;
                vx = 1;
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }

    /*
    ---------------------------------------------------


            -- MULTIPLAYER METHODS AND STUFF --


     --------------------------------------------------
     */

    private DatagramSocket socket;
    private UDP_Conn connToServer;
    private ArrayList<SnakeData> playerData;
    private boolean online = false;

    public void establishConnection() {
        if(socket == null) {
            playerData = new ArrayList<>();
            try {
                socket = new DatagramSocket(4999);
                connToServer = new UDP_Conn(InetAddress.getByName("jack-mint"), 5000);
                //Get on server list of connections
                connToServer.sendMessage(socket, new SnakeData(null, 0, 0, 0));
            } catch (SocketException | UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public void waitForGameStart() {
        SnakeData data;
        do {
            data = connToServer.receiveMessage(socket);
        } while(!data.GamePlaying);
    }

    public void sendData() {
        connToServer.sendMessage(socket, new SnakeData(snake, vx, vy, 0));
    }

    //Receive data from server and display it.
    public void processData() {
        playerData.clear();
        int playerNum = 0;
        do {
            SnakeData data = connToServer.receiveMessage(socket);
            playerData.add(data);
            playerNum = data.playerAmount;
        } while(playerData.size() < playerNum);
        food = playerData.get(0).food;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        socket.close();
    }
}
