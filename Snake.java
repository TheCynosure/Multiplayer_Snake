import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by jack on 6/30/17.
 */
public class Snake extends JApplet implements KeyListener {
    int pixelSize = 25;
    int gridSize = 20;
    int vx, vy;
    Random random;
    ArrayList<Point> snake;
    Point food;
    long pastTime, timeSinceUpdate, speed;
    public void init() {
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
        h.setColor(Color.GREEN);
        for(Point p: snake) {
            h.fillRect(p.x * pixelSize, p.y * pixelSize, pixelSize, pixelSize);
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
        if(timeSinceUpdate > speed) {
            move();
            timeSinceUpdate -= speed;
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

    ByteArrayOutputStream byteArrayOutputStream;
    ObjectOutputStream objectOutputStream;
    ObjectInputStream objectInputStream;
    ByteArrayInputStream byteArrayInputStream;
    UDP_Conn connToServer;
    DatagramSocket socket;
    int numPlayers = 0;
    byte[] inputData;

    public void setupMultiplayer() {
        byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            byteArrayInputStream = new ByteArrayInputStream(inputData);
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            connToServer = new UDP_Conn(InetAddress.getByName("WhyYouLookingAtMe"), 5000);
            socket = new DatagramSocket(4999);
            //SETUP CONNECTION WITH SERVER
            String message = "Hello there server, I'm Jack";
            connToServer.sendMessage(socket,  message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getSnakeMsg() {
        SnakeData snakeData = new SnakeData(snake, vx, vy);
        byteArrayOutputStream.reset();
        try {
            objectOutputStream.writeObject(snakeData);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return byteArrayOutputStream.toByteArray();
    }

    public void sendData() {
        connToServer.sendMessage(socket, getSnakeMsg());
    }


    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        socket.close();
        objectOutputStream.close();
        byteArrayOutputStream.close();
    }

    public int GameBegun() {
        String response = "";
        if((response = connToServer.receiveMessage(socket)).contains("GAME START")) {
            String[] responseParts = response.split(" ");
            numPlayers = Integer.parseInt(responseParts[2]);
            return numPlayers;
        }
        return -1;
    }

    //RECEIVING DATA METHODS

    //TODO PARSE INCOMING DATA
    public void receiveData() {
        int receivedPlayers = 0;
        while(receivedPlayers < numPlayers) {
            String recData = connToServer.receiveMessage(socket);
            inputData = recData.getBytes();
            try {
                SnakeData data = (SnakeData) objectInputStream.readObject();
                for(Point point: data.snake) {
                    snake.add(point);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
