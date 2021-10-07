package Game;

import java.util.ArrayList;

public class Array2D<T> {
    ArrayList<ArrayList<T>> array;
    public final int width, height;
    public final Point dimensions;

    public Array2D(int width, int height, T defaultValue) {
        this.width=width;
        this.height=height;
        this.dimensions = new Point(width, height);
        array = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            var a = new ArrayList<T>(height);
            for (int j = 0; j < height; j++) {
                a.add(defaultValue);
            }
            array.add(a);
        }
    }
    public Array2D(int width, int height){
        this(width,height,null);
    }

    public Array2D(Point dimensions, T defaultValue) {
        this(dimensions.x(), dimensions.y(), defaultValue);
    }
    public Array2D(Point dimensions) {
        this(dimensions,null);
    }

    public void set(int x, int y, T value){
        array.get(x).set(y, value);
    }
    public void set(Point index, T value) {
        set(index.x(), index.y(), value);
    }
    public T get(int x, int y) {
        return array.get(x).get(y);
    }
    public T get(Point index){
        return get(index.x(), index.y());
    }
}
