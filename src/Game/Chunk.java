package Game;

import UI.PastingMode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class Chunk {
    public static final Point chunkDimensions = new Point(32, 32);
    //probably easier to update with not overlapped
    @Deprecated
    public static final Point chunkDimensionsOverlapped = chunkDimensions.add(0);
    public static final Paint blockPaint = Paint.valueOf("111111");
    //now handled as transformation(scale)
    @Deprecated
    public static final Point blockDimensions = new Point(1, 1);
    @Deprecated
    public static final double extraRenderMargin = 0;
    public static SortedMap<Point, Chunk> chunks = new TreeMap<>();
    public static Pane drawPane;
    public static String levelDir = "resources/levels/";
    public static Pane transformPane;
    private static int turn = 0;
    private static final List<Point> chunksToAdd = new LinkedList<>();
    public final Point position;
    //    private final b<Rectangle> rectangles = new Array2D<>(chunkDimensions);
    private final Rectangle[][] rectangles = new Rectangle[chunkDimensions.x()][chunkDimensions.y()];
    //    private final Array2D<Boolean> blocks = new Array2D<>(chunkDimensions, false);
//    private final Array2D<Boolean> blockMask = new Array2D<>(chunkDimensions.add(2), false);
    private final boolean[][] blocks = new boolean[chunkDimensions.x()][chunkDimensions.y()];
    private final boolean[][] blockMask = new boolean[chunkDimensions.x() + 2][chunkDimensions.y() + 2];
    // 5 6 7
    // 3 - 4
    // 0 1 2 Neighbours, Opposite is 7-x
    private final Chunk[] neighbours = new Chunk[8];
    private final Point[] neighbourMask = new Point[]{
            new Point(-1, -1), new Point(0, -1), new Point(1, -1),
            new Point(-1, 0), new Point(1, 0),
            new Point(-1, 1), new Point(0, 1), new Point(1, 1)
    };
    public boolean drawChunks = false;
    Rectangle chunkRectangle;
    private int blockCount;
    private int turnsToRemoveChunk = 50; // how many turns until emptyChunk removed
    private final Pane chunkPane;

    public Chunk(Point position) {
        chunkPane = new Pane();
        chunkPane.setLayoutY(0);
        chunkPane.setLayoutX(0);
        chunkPane.setPrefWidth(0);
        chunkPane.setPrefHeight(0);
        drawPane.getChildren().add(chunkPane);
        this.position = position;
        chunks.put(position, this);
        for (int i = 0; i < 8; i++) {
            neighbours[i] = chunks.get(position.add(neighbourMask[i]));
            if (neighbours[i] != null) {
                neighbours[i].neighbours[7 - i] = this;
            }
        }

        if (drawChunks) {
            Point p = fromChunkToBlock(new Point(0, 0), position).times(blockDimensions);
            Point w = chunkDimensions.times(blockDimensions);
            chunkRectangle = new Rectangle(p.x(), p.y(), w.x(), w.y());
            chunkRectangle.fillProperty().setValue(Paint.valueOf("FF0000"));
            drawPane.getChildren().add(chunkRectangle);
            chunkRectangle.toBack();
        }
    }

    public static void clear() {
        turn = 0;
        chunks.clear();
        drawPane.getChildren().clear();
    }

    public static void toggleBlock(Point blockPosition) {
        Chunk c = getChunkFromBlock(blockPosition);
        c.toggleBlockInChunk(fromBlockToInChunk(blockPosition, c.position));
        addChunks();
    }
    public static void addBlock(Point blockPosition){
        Chunk c = getChunkFromBlock(blockPosition);
        c.addBlockInChunk(fromBlockToInChunk(blockPosition, c.position));
        addChunks();
    }
    public static void removeBlock(Point blockPosition){
        Chunk c = getChunkFromBlock(blockPosition);
        c.removeBlockInChunk(fromBlockToInChunk(blockPosition, c.position));
        addChunks();
    }

    public static void update() {
        turn += 1;
        chunks.values().forEach(Chunk::setMask);
        chunks.values().forEach(Chunk::updateChunk);
        addChunks();
        removeChunks();
    }

    public static void addChunks() {
        for (Point p : chunksToAdd) {
            getChunk(p);
        }
        chunksToAdd.clear();
    }

    public static void removeChunks() {
        List<Chunk> toRemove = new LinkedList<>();
        for (Chunk chunk : chunks.values()) {
            if (chunk.turnsToRemoveChunk <= 0) {
                chunk.prepareRemoval();
                toRemove.add(chunk);
            }
        }
        for (Chunk chunk : toRemove) {
            chunks.remove(chunk.position);
        }
    }

    public static Chunk getChunkFromBlock(Point blockPosition) {
        return getChunk(fromBlockToChunk(blockPosition));
    }

    public static Chunk getChunk(Point chunkPosition) {
        Chunk c = chunks.get(chunkPosition);
        if (c == null) {
            c = new Chunk(chunkPosition);
        }
        return c;
    }

    public static Point fromBlockToChunk(Point p) {
        return p.castToDouble().divide(chunkDimensionsOverlapped.castToDouble()).floor().castToInt();
    }

    public static Point fromBlockToInChunk(Point blockPosition, Point chunkPosition) {
        return blockPosition.subtract(chunkPosition.times(chunkDimensionsOverlapped));
    }

    public static Point fromPositionToBlock(Pointd pos) {
        return pos.divide(blockDimensions.castToDouble()).floor().castToInt();
    }

    public static Point fromChunkToBlock(Point block, Point chunk) {
        return chunk.times(chunkDimensionsOverlapped).add(block);
    }

    public static int getTurn() {
        return turn;
    }

    public static void save(String name) throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(Path.of(levelDir + name), StandardOpenOption.CREATE));
        stream.writeInt(chunks.size());
        for (Chunk c : chunks.values()) {
            c.writeToStream(stream);
        }
        stream.flush();
        stream.close();
    }

    public static void load(String name) throws IOException {
        clear();
        ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(Path.of(levelDir + name), StandardOpenOption.READ));
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
            Chunk c = new Chunk(Point.fromStream(stream));
            int blocks = stream.readInt();
            for (int j = 0; j < blocks; j++) {
                c.addBlockInChunk(Point.fromStream(stream));
            }
        }
        stream.close();
    }

    public static void removeBlocks(Point start, Point end) {
        var minx = Math.min(start.x(), end.x());
        var miny = Math.min(start.y(), end.y());
        var maxx = Math.max(start.x()+1, end.x()+1);
        var maxy = Math.max(start.y()+1, end.y()+1);
        for (int i = minx; i <= maxx; i++) {
            for (int j = miny; j <= maxy; j++) {
                removeBlock(new Point(i,j));
            }
        }
    }
    public static void removeBlocks(Point start, boolean[][] blocks) {
        for (int i = 0; i < blocks.length; i++) {
            for (int j = 0; j < blocks[0].length; j++) {
                var p = start.add(new Point(i, j));
                removeBlock(p);
            }
        }
    }

    public void toggleBlockInChunk(Point blockPositionInChunk) {
        int x = blockPositionInChunk.x();
        int y = blockPositionInChunk.y();
        boolean b = blocks[x][y];
        if (!b) {
            addBlockInChunk(blockPositionInChunk);
        } else {
            removeBlockInChunk(blockPositionInChunk);
        }
    }

    public void addBlockInChunk(Point blockPositionInChunk) {
        int x = blockPositionInChunk.x();
        int y = blockPositionInChunk.y();
        boolean b = blocks[x][y];
        if (!b) {
            Point pos = fromChunkToBlock(blockPositionInChunk, position);
            blocks[x][y] = true;
            blockCount += 1;

            Rectangle r = rectangles[x][y];
            if (r == null) {
                r = spawnRectangle(blockPositionInChunk, pos);
                chunkPane.getChildren().add(r);
            } else {
                r.setVisible(true);
            }
//            add neighbor chunks
            Point p = position.add(blockPositionInChunk.times(2).add(chunkDimensions.add(-1).times(-1)).divide(chunkDimensions.add(-1)));
            if (!p.equals(position))
                scheduleChunk(p);
        }
    }

    private Rectangle spawnRectangle(Point blockPositionInChunk, Point pos) {
        Rectangle r;
        Point posOnMap = pos.times(blockDimensions);
        r = new Rectangle(posOnMap.x() - extraRenderMargin, posOnMap.y() - extraRenderMargin,
                blockDimensions.x() + 2 * extraRenderMargin, blockDimensions.y() + 2 * extraRenderMargin);
        r.fillProperty().setValue(blockPaint);
        r.setDisable(true);
        rectangles[blockPositionInChunk.x()][blockPositionInChunk.y()] = r;
        return r;
    }

    private void scheduleChunk(Point p) {
        chunksToAdd.add(p);
    }

    public void removeBlockInChunk(Point blockPositionInChunk) {
        int x = blockPositionInChunk.x();
        int y = blockPositionInChunk.y();
        boolean b = blocks[x][y];
        if (b) {
            var r = rectangles[x][y];
//            chunkPane.getChildren().remove(r);
            blocks[x][y] = false;
            blockCount -= 1;
//            rectangles[blockPositionInChunk.x()][blockPositionInChunk.y()] = null;
            r.setVisible(false);
        }
    }

    private void scheduleRemoval() {
        turnsToRemoveChunk -= 1;
    }

    private void prepareRemoval() {
//        chunkPane.getChildren().clear();
        drawPane.getChildren().remove(chunkPane);
        drawPane.getChildren().remove(chunkRectangle);
        for (int i = 0; i < 8; i++) {
            Chunk c = neighbours[i];
            if (c != null) c.neighbours[7 - i] = null;
        }
    }

    private void setMask() {
        int x = blocks.length - 1;
        int y = blocks[0].length - 1;
        int u = blockMask.length - 1;
        int v = blockMask[0].length - 1;
        //corners
        blockMask[0][0] = neighbours[0] != null && neighbours[0].blocks[x][y];
        blockMask[u][0] = neighbours[2] != null && neighbours[2].blocks[0][y];
        blockMask[0][v] = neighbours[5] != null && neighbours[5].blocks[x][0];
        blockMask[u][v] = neighbours[7] != null && neighbours[7].blocks[0][0];
        //edges
        for (int i = 0; i < u - 1; i++) {
            blockMask[i + 1][0] = neighbours[1] != null && neighbours[1].blocks[i][y];
        }
        for (int i = 0; i < v - 1; i++) {
            blockMask[0][i + 1] = neighbours[3] != null && neighbours[3].blocks[x][i];
        }
        for (int i = 0; i < v - 1; i++) {
            blockMask[u][i + 1] = neighbours[4] != null && neighbours[4].blocks[0][i];
        }
        for (int i = 0; i < u - 1; i++) {
            blockMask[i + 1][v] = neighbours[6] != null && neighbours[6].blocks[i][0];
        }
        //inner
        for (int i = 0; i < chunkDimensions.x(); i++) {
            for (int j = 0; j < chunkDimensions.y(); j++) {
                blockMask[i + 1][j + 1] = blocks[i][j];
            }
        }
    }

    private void updateChunk() {
        boolean empty = true;
        for (int j = 0; j < chunkDimensions.y(); j++) {
            for (int i = 0; i < chunkDimensions.x(); i++) {
                int count = 0;
                Point pos = new Point(i, j);
                for (int k = 0; k < 8; k++) {
                    Point p = pos.add(neighbourMask[k]);
                    if (blockMask[p.x() + 1][p.y() + 1]) {
                        empty = false;
                        count += 1;
                    }
                }
                if (count < 2 || count > 3) {
                    removeBlockInChunk(pos);
                }
                if (count == 3) {
                    addBlockInChunk(pos);
                }
            }
        }
        if (empty) {
            scheduleRemoval();
        }
    }

    private void writeToStream(ObjectOutputStream stream) throws IOException {
        position.writeToStream(stream);
        stream.writeInt(blockCount);
        int count = 0;
        for (int i = 0; i < chunkDimensions.x(); i++) {
            for (int j = 0; j < chunkDimensions.y(); j++) {
                if (blocks[i][j]) {
                    var p = new Point(i, j);
                    p.writeToStream(stream);
                    count++;
                }
            }
        }
        assert count == blockCount;
    }

    public static boolean[][] getSelection(Point start, Point end) {
        var minx = Math.min(start.x(), end.x());
        var miny = Math.min(start.y(), end.y());
        var maxx = Math.max(start.x()+1, end.x()+1);
        var maxy = Math.max(start.y()+1, end.y()+1);
        int height = maxy - miny;
        int width = maxx - minx;
        boolean[][] a = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                a[i][j] = getBlock(new Point(minx,miny).add(new Point(i,j)));
            }
        }
        return a;
    }

    public static boolean getBlock(Point block) {
        var c= getChunkFromBlock(block);
        var b = fromBlockToInChunk(block,c.position);
        return c.blocks[b.x()][b.y()];
    }

    public static void addBlocks(Point start, boolean[][] blocks, PastingMode pastingMode) {
        for (int i = 0; i < blocks.length; i++) {
            for (int j = 0; j < blocks[0].length; j++) {
                var p = start.add(new Point(i, j));
                switch (pastingMode) {
                    case replace -> {
                        if (blocks[i][j]) {
                            addBlock(p);
                        }else{
                            removeBlock(p);
                        }
                    }
                    case add -> {
                        if (blocks[i][j]) {
                            addBlock(p);
                        }
                    }
                    case subtract -> {
                        if (blocks[i][j]) {
                            removeBlock(p);
                        }
                    }
                    case toggle ->{
                        if(blocks[i][j]){
                            toggleBlock(p);
                        }
                    }
                }
            }
        }
    }
}
