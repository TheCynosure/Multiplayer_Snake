import org.omg.CORBA.Object;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

public class SnakeData implements Serializable {
        protected ArrayList<Point> snake;
        protected int vx, vy;

        public SnakeData(ArrayList<Point> snake, int vx, int vy) {
            this.snake = snake;
            this.vx = vx;
            this.vy = vy;
        }
}
