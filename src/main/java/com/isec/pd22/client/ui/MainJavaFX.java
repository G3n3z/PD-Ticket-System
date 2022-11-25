package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;

public class MainJavaFX extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        ModelManager modelManager = new ModelManager();

        List<String> argsList =  getParameters().getRaw();
        String [] args = new String[argsList.size()];
        for (int i = 0; i < argsList.size(); i++) {
            args[i] = argsList.get(i);
        }
        RootPane rootPane = new RootPane(modelManager, stage, args);
        Scene scene = new Scene(rootPane, 1600,800);
        stage.setScene(scene);
        stage.setTitle("APP do catano");
        stage.show();
        //rootPane.startServices();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
