import org.omg.CORBA.Object;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;

public class SnakeData implements Serializable {
        protected boolean GamePlaying = false;
        protected Point food;
        protected ArrayList<Point> snake;
        protected int vx, vy, playerAmount;
        protected Color color = Color.BLACK;

        public SnakeData(ArrayList<Point> snake, int vx, int vy, int playerAmount) {
            this.snake = snake;
            this.vx = vx;
            this.vy = vy;
            this.playerAmount = playerAmount;
        }
}
