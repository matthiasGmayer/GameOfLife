package Game;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

public record Point(int x, int y) implements Comparable<Point>, Serializable {

    @Override
    public int compareTo(Point o) {
        if (x < o.x || x == o.x && y < o.y) return -1;
        else if(x == o.x && y == o.y) return 0;
        else return 1;
    }
    public Point add(Point p){
        return new Point(p.x+x,p.y+y);
    }
    public Point add(int i){
        return new Point(x+i,y+i);
    }
    public Point times(Point p){
        return new Point(p.x*x,p.y*y);
    }
    public Point times(int i){
        return new Point(x*i,y*i);
    }

    public Point divide(Point p) {
//            Already Implemented by default
//            if(p.x == 0 || p.y == 0)
//                throw new ArithmeticException("Divide by 0");
        return new Point(x/p.x,y/p.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public Pointd castToDouble() {
        return new Pointd(x,y);
    }

    public Point subtract(Point p) {
        return new Point(x-p.x,y-p.y);
    }

    public void writeToStream(ObjectOutputStream stream) throws IOException {
        stream.writeInt(x);
        stream.writeInt(y);
    }
    public static Point fromStream(ObjectInputStream stream) throws IOException {
        return new Point(stream.readInt(),stream.readInt());
    }

    public Point subtract(int i) {
        return new Point(x - i, y - i);
    }

    public Point subtract(int x, int y) {
        return new Point(x()-x,y()-y);
    }
}