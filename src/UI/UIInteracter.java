package UI;

import FXML.MainPaneController;
import Game.Chunk;
import Game.Point;
import Game.Pointd;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class UIInteracter {
    private final int startZoomLevel = 4;
    private final double[] zoomLevels = new double[]{
//            0.1, 0.2, 0.4, 0.7, 1, 2, 3, 4, 6, 9, 14, 20
            1, 2, 4, 7, 10, 20, 30, 40, 60, 90, 140, 200
    };
    private boolean keyReleased = true;

    enum Mode {
        manipulation,
        selecting,
        selected,
        pasting
    }

    private final Pane transformPane;
    private final MainPaneController mainPaneController;
    private final Rectangle clickRectangle;
    private int zoom;
    private Pointd onDragEnteredOnMap = new Pointd(0, 0);
    private Pointd panePositionBuffer = new Pointd(0, 0);
    private Text turnText;
    private Text pastingModeText;
    private Pointd internalTransformPanePosition = new Pointd(0, 0);
    private Pointd mouse = new Pointd(0,0);
    private final Rectangle selectionRectangle;
    private final Group pastingGroup;
    private Point selectionEndBlock = new Point(0, 0);
    private Point selectionStartBlock = new Point(0, 0);
    private final Paint insertPaint = Paint.valueOf("80000060");
    private final Paint selectionPaint = Paint.valueOf("00008060");

    private boolean[][] selectionBlocks;
    private boolean[][][] numberTemplates = new boolean[10][][];
    private boolean isScrolling;

    private Mode state = Mode.manipulation;
    private PastingMode pastingMode = PastingMode.add;
    private ManipulationMode manipulationMode = ManipulationMode.toggle;

    public UIInteracter(MainPaneController controller, Scene scene) {

        zoom = startZoomLevel;

        this.mainPaneController = controller;
        Pane interactionPane = controller.interactionPane;
        Pane uiPane = controller.uiPane;
        this.transformPane = controller.transformPane;
        clickRectangle = new Rectangle(0, 0, 1, 1);
        clickRectangle.fillProperty().setValue(insertPaint);
        selectionRectangle = new Rectangle(0, 0, 0, 0);
        selectionRectangle.fillProperty().setValue(selectionPaint);
        pastingGroup = new Group();

        interactionPane.getChildren().add(clickRectangle);
        interactionPane.getChildren().add(selectionRectangle);
        interactionPane.getChildren().add(pastingGroup);


        uiPane.setOnMouseMoved(mouseEvent -> {
            mouse = fromEventToMouse(mouseEvent);
            if (state == Mode.manipulation)
                setClickRectangle(mouse);
            else if (state == Mode.pasting) {
                setPastingGroup();
            }
        });
        uiPane.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                if (state == Mode.manipulation&&manipulationMode== ManipulationMode.toggle) {
                    Chunk.toggleBlock(getBlock(mouseEvent));
                } else if (state == Mode.selected) {
                    setState(Mode.manipulation);
                }
                if (state == Mode.pasting) {
                    Chunk.addBlocks(getBlock(mouse).subtract(pastingBlocks.length / 2, pastingBlocks[0].length / 2), pastingBlocks, pastingMode);
                    if (keyReleased)
                        setState(Mode.manipulation);
                }
            }
        });
        uiPane.setOnScroll(scrollEvent -> {
            if (scrollEvent.getDeltaY() == 0) return;
            var mousePos = fromEventToMouse(scrollEvent);
            var posPrev = fromMouseToMap(mousePos);
            zoom = Math.min(zoomLevels.length - 1, Math.max(0, zoom + (int) Math.signum(scrollEvent.getDeltaY())));
            var newMousePos = fromMapToMouse(posPrev);
            var toMoveOnScreen = mousePos.subtract(newMousePos);
            transformPane.setScaleX(getZoom());
            transformPane.setScaleY(getZoom());
            setTransformPanePosition(internalTransformPanePosition.add(toMoveOnScreen));
        });
        uiPane.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                selectionStartBlock = getBlock(mouseEvent);
                setState(Mode.selecting);
            } else if (mouseEvent.getButton() == MouseButton.MIDDLE) {
                if (!isScrolling) {
                    isScrolling = true;
                    onDragEnteredOnMap = fromMouseToMap(mouse);
                    panePositionBuffer = getTransformPanePosition();
                }
            }
        });
        uiPane.setOnMouseDragged(mouseEvent -> {
            mouse = fromEventToMouse(mouseEvent);
            if (isScrolling) {
                setTransformPanePosition(fromMapToMouse(fromMouseToMap(fromEventToMouse(mouseEvent), panePositionBuffer).subtract(onDragEnteredOnMap), panePositionBuffer));
            } else if (state == Mode.selecting) {
                selectionEndBlock = getBlock(mouseEvent);
                setSelectionRectangle(selectionStartBlock, selectionEndBlock);
            }
            if (state == Mode.manipulation && mouseEvent.getButton() == MouseButton.PRIMARY) {
                if (manipulationMode == ManipulationMode.brush) {
                    Chunk.addBlock(getBlock(mouseEvent));
                } else if (manipulationMode == ManipulationMode.eraser) {
                    Chunk.removeBlock(getBlock(mouseEvent));
                }
            }
        });
        uiPane.setOnMouseReleased(mouseEvent -> {
            if (mouseEvent.getButton() == MouseButton.MIDDLE) {
                isScrolling = false;
            }
            if (state == Mode.selecting) {
                setState(Mode.selected);
            }
        });


        initDebugs();
        scene.setOnKeyReleased(keyEvent -> keyReleased = true);
        scene.setOnKeyPressed(keyEvent -> {
            var code = keyEvent.getCode();
            var character = keyEvent.getText();
            if (code != KeyCode.SPACE && !keyReleased) return;
            keyReleased = false;
            switch (character) {
                case "r":
                    Chunk.clear();
                    zoom = startZoomLevel;
                    setTransformPanePosition(new Pointd(0, 0));
                    setDebugs();
                    break;
                case "s":
                    try {
                        Chunk.save("test.lvl");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "l":
                    try {
                        Chunk.load("test.lvl");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case "d":
                    if (state == Mode.selected) {
                        Chunk.removeBlocks(selectionStartBlock, selectionEndBlock);
                        setState(Mode.manipulation);
                    }
                case "v":
                    if (selectionBlocks != null) {
                        initPastinGroup(selectionBlocks);
                        setPastingGroup();
                        setState(Mode.pasting);
                    }
                    break;
                case "c":
                    if (state == Mode.selected) {
                        selectionBlocks = Chunk.getSelection(selectionStartBlock, selectionEndBlock);
//                        initPastinGroup(selectionBlocks);
                        setState(Mode.manipulation);
                    }
                    break;
                case "b":
                    if (state == Mode.pasting) {
                        pastingMode = PastingMode.values()[(pastingMode.ordinal() + 1) % PastingMode.values().length];
                    }else if(state == Mode.manipulation){
                        manipulationMode = ManipulationMode.values()[(manipulationMode.ordinal() + 1) % ManipulationMode.values().length];
                    }
                    setModeText();
                    break;
                default:
                    try {
                        int num = Integer.parseInt(character);
                        System.out.println(num);
                        if (state == Mode.selected) {
                            numberTemplates[num] = Chunk.getSelection(selectionStartBlock, selectionEndBlock);
//                            initPastinGroup(numberTemplates[num]);
                            try {
                                saveSelection(Integer.toString(num), numberTemplates[num]);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            setState(Mode.manipulation);
                        } else if (state == Mode.manipulation || state == Mode.pasting) {
                            if (numberTemplates[num] != null) {
                                initPastinGroup(numberTemplates[num]);
                                setPastingGroup();
                                setState(Mode.pasting);
                            }
                        }
                    } catch (NumberFormatException e) {
                        switch (code) {
                            case ESCAPE:
                                setState(Mode.manipulation);
                                break;
                            case SPACE:
                                Chunk.update();
                                setDebugs();
                                break;
                        }
                    }
                    break;
            }
        });

        for (int i = 0; i < 10; i++) {
            try {
                numberTemplates[i] = loadSelection(Integer.toString(i));
            } catch (IOException e) {
                System.out.println("Template " + i + " not loaded");
                e.printStackTrace();
            }
        }

        setModeText();
    }

    private void initPastinGroup(boolean[][] pastingBlocks) {
        if (this.pastingBlocks == pastingBlocks) {
            setPastingGroup();
        } else {
            this.pastingBlocks = pastingBlocks;
            pastingGroup.getChildren().clear();
            for (int i = 0; i < pastingBlocks.length; i++) {
                for (int j = 0; j < pastingBlocks[i].length; j++) {
                    if (pastingBlocks[i][j]) {
                        var r = new Rectangle(i, j, 1, 1);
                        r.fillProperty().setValue(insertPaint);
                        pastingGroup.getChildren().add(r);
                    }
                }

            }
        }
    }

    boolean[][] pastingBlocks;

    private void setPastingGroup() {
        var b = getBlock(mouse).subtract(pastingBlocks.length / 2, pastingBlocks[0].length / 2);
        pastingGroup.setTranslateX(b.x());
        pastingGroup.setTranslateY(b.y());
    }

    private void setState(Mode mode) {
        if (mode == Mode.manipulation) {
            clickRectangle.setVisible(true);
            setClickRectangle(mouse);
        } else {
            clickRectangle.setVisible(false);
        }

        if (mode == state) return;

        if (state == Mode.selected) {
            selectionRectangle.setVisible(false);
        }
        if (mode == Mode.selecting) {
            selectionRectangle.setVisible(true);
            setSelectionRectangle(selectionStartBlock, selectionStartBlock);
        }
        boolean isPasting = mode == Mode.pasting;
        pastingGroup.setVisible(isPasting);

        state = mode;
        setModeText();
    }

    private void setModeText() {
        String s = state.toString();
        if(state == Mode.pasting){
            s += ": " + pastingMode;
        } else if (state == Mode.manipulation) {
            s += ": " + manipulationMode;
        }
        pastingModeText.setText(s);
    }

    private void setClickRectangle(Pointd mouse) {
        Point blockPosition = getBlock(mouse);
        clickRectangle.setX(blockPosition.x());
        clickRectangle.setY(blockPosition.y());
    }

    private Point getBlock(Pointd mouse) {
        return Chunk.fromPositionToBlock(fromMouseToMap(mouse));
    }

    private void setSelectionRectangle(Point start, Point end) {
        var minx = Math.min(start.x(), end.x());
        var miny = Math.min(start.y(), end.y());
        var maxx = Math.max(start.x() + 1, end.x() + 1);
        var maxy = Math.max(start.y() + 1, end.y() + 1);
        var x = maxx - minx;
        var y = maxy - miny;
        selectionRectangle.setX(minx);
        selectionRectangle.setY(miny);
        selectionRectangle.setWidth(x);
        selectionRectangle.setHeight(y);
    }

    private Point getBlock(MouseEvent mouseEvent) {
        return getBlock(fromEventToMouse(mouseEvent));
    }

    private void initDebugs() {
        turnText = mainPaneController.turnText;
        pastingModeText = mainPaneController.pastingModeText;
    }

    private void setDebugs() {
        turnText.setText(Integer.toString(Chunk.getTurn()));
    }

    Pointd fromMouseToMap(Pointd mouse, Pointd panePosition) {
        return mouse.subtract(panePosition).divide(getZoom());
    }

    Pointd fromMouseToMap(Pointd mouse) {
        return mouse.subtract(internalTransformPanePosition).divide(getZoom());
    }

    Pointd fromMapToMouse(Pointd p, Pointd panePosition) {
        return p.times(getZoom()).add(panePosition);
    }

    Pointd fromMapToMouse(Pointd p) {
        return fromMapToMouse(p, internalTransformPanePosition);
    }

    Pointd fromEventToMouse(MouseEvent mouseEvent) {
        return new Pointd(mouseEvent.getX(), mouseEvent.getY());
    }

    Pointd fromEventToMouse(ScrollEvent mouseEvent) {
        return new Pointd(mouseEvent.getX(), mouseEvent.getY());
    }

    Pointd getTransformPanePosition() {
        return internalTransformPanePosition;
    }

    void setTransformPanePosition(Pointd pos) {
        Point p = pos.castToInt();
        transformPane.setTranslateX(p.x());
        transformPane.setTranslateY(p.y());
        internalTransformPanePosition = p.castToDouble();
    }

    public double getZoom() {
        return zoomLevels[zoom];
    }


    public final String templateDir = "resources/templates/";

    public void saveSelection(String name, boolean[][] blocks) throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(Path.of(templateDir + name), StandardOpenOption.CREATE));
        stream.writeInt(blocks.length);
        stream.writeInt(blocks[0].length);
        for (int i = 0; i < blocks.length; i++) {
            for (int j = 0; j < blocks[i].length; j++) {
                stream.writeBoolean(blocks[i][j]);
            }
        }
        stream.flush();
        stream.close();
    }

    public boolean[][] loadSelection(String name) throws IOException {
        ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(Path.of(templateDir + name)));
        int width = stream.readInt();
        int height = stream.readInt();
        boolean[][] b = new boolean[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                b[i][j] = stream.readBoolean();
            }
        }
        stream.close();
        return b;
    }
}
