package UI;

import Game.Chunk;
import Game.Point;
import Game.Pointd;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import FXML.MainPaneController;

import java.util.Objects;

public class Main extends Application {
    public static Stage stage;
    public static Scene scene;

    @Override
    public void start(Stage primaryStage) throws Exception{
        stage= primaryStage;
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(Objects.requireNonNull(getClass().getResource("../FXML/MainFrame.fxml")));
        Pane mainPane = loader.load();
        MainPaneController controller = loader.getController();
        Pane drawPane = controller.drawPane;
        Chunk.drawPane = drawPane;
        Chunk.transformPane = controller.transformPane;
        primaryStage.setTitle("Game Of Life With Chunks!");
        scene = new Scene(mainPane);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setMaximized(true);
        var u = new UIInteracter(controller,scene);
//        u.setDrawPanePosition(new Pointd(primaryStage.getWidth()/2,primaryStage.getHeight()/2));

        int w= 100;
        for (int i = 0; i < w; i++) {
            for (int j = 0; j < w; j++) {
                if((i+j)%2==0)
                    Chunk.toggleBlock(new Point(i,j));
//                if(Math.random()>0.5)
//                    Chunk.toggleBlock(new Point(i,j));
            }
        }
//        for (int i = 0; i < 450; i++) {
//            Chunk.update();
//        }
    }

    public static void launch(String ... args){
        Application.launch(args);
    }


}
