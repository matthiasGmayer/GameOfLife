package UI;

import Game.Array2D;

import java.util.Random;
import java.util.Timer;

public class Start {
    public static void main(String[] args) {
        Main.launch(args);
    }

    public static void testArray(){
        int w = 20000;
        Array2D<Boolean> b = new Array2D<>(w,w,false);
        int[] accesesx = new int[10000];
        int[] accesesy = new int[accesesx.length];
        boolean[] bool = new boolean[accesesx.length];
        Random random = new Random();
        for (int i = 0; i < accesesx.length; i++) {
            int bound = w - 2;
            accesesx[i] = random.nextInt(bound);
            accesesy[i] = random.nextInt(bound);
            bool[i] = random.nextBoolean();
        }
        var b2 = new boolean[accesesx.length][accesesx.length];

        var l = System.nanoTime();
        for (int i = 0; i < accesesx.length; i++) {
            boolean a = b2[accesesx[i]][accesesy[i]];
            b2[accesesx[i]][accesesy[i]]=bool[i] && a;
        }
        var l2 = System.nanoTime()-l;
        System.out.println(l2);

        l = System.nanoTime();
        for (int i = 0; i < accesesx.length; i++) {
            boolean a = b.get(accesesx[i],accesesy[i]);
            b.set(accesesx[i],accesesy[i],bool[i] && a);
        }
        l2 = System.nanoTime()-l;
        System.out.println(l2);
    }
}
