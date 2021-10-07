package Game;

import javafx.geometry.Point2D;

import java.util.Objects;

public record Pointd(double x, double y) implements Comparable<Pointd> {
    @Override
    public int compareTo(Pointd o) {
        if (x < o.x || x == o.x && y < o.y) return -1;
        else if(x == o.x && y == o.y) return 0;
        else return 1;
    }
    public Pointd add(Pointd p){
        return new Pointd(p.x+x,p.y+y);
    }
    public Pointd add(double i){
        return new Pointd(x+i,y+i);
    }
    public Pointd subtract(Pointd p){
        return new Pointd(x-p.x,y-p.y);
    }
    public Pointd subtract(double i) {
        return new Pointd(x - i, y - i);
    }
    public Pointd times(Pointd p){
        return new Pointd(p.x*x,p.y*y);
    }
    public Pointd times(double i){
        return new Pointd(x*i,y*i);
    }

    public Pointd divide(Pointd p) {
//            Already Implemented by default
//            if(p.x == 0 || p.y == 0)
//                throw new ArithmeticException("Divide by 0");
        return new Pointd(x/p.x,y/p.y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pointd point = (Pointd) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    public Pointd divide(double zoom) {
        return new Pointd(x/zoom,y/zoom);
    }

    public Point castToInt() {
        return new Point((int)x,(int)y);
    }
    public static Pointd fromFx(Point2D p){
        return new Pointd(p.getX(),p.getY());
    }
    public Point2D toFx(){
        return new Point2D(x,y);
    }

    public Pointd floor() {
        return new Pointd(Math.floor(x),Math.floor(y));
    }
}