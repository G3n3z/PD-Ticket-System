package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.ClientMSG;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;


import java.util.List;

public class LogInView extends BorderPane {

    ModelManager modelManager;
    HBox hbox;
    VBox vBox;
    Label label, label2;
    TextField tfEmail, tfPassword;
    Button btnLogin, btnRegister;

    public LogInView(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
        updateView();
    }


    private void createViews() {
        hbox = new HBox();
        vBox = new VBox();
        Label label1 = new Label("Bem-vindo");
        label1.setFont(new Font(30));
        HBox hBoxTitle = new HBox(label1);
        hBoxTitle.setAlignment(Pos.CENTER);
        HBox.setMargin(label1, new Insets(0,0,50,0));
        //setTop(hBoxTitle);
        label = new Label("Email");
        label.setFont(new Font(20));
        tfEmail = new TextField();
        tfEmail.setPrefHeight(30);
        tfEmail.setPrefWidth(200);
        label2 = new Label("Password");
        label2.setFont(new Font(20));
        tfPassword = new TextField();
        tfPassword.setPrefHeight(30);
        tfPassword.setPrefWidth(200);
        vBox.getChildren().addAll(hBoxTitle,label, tfEmail, label2, tfPassword);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(20);
        vBox.setMinWidth(500);
        VBox.setMargin(tfEmail, new Insets(5,0,30,0));
        hbox.getChildren().addAll(vBox);
        hbox.setAlignment(Pos.CENTER);
        btnLogin = new Button("Login");
        btnRegister = new Button("Registar");
        btnRegister.setPrefWidth(100);btnRegister.setPrefHeight(60);
        btnLogin.setPrefWidth(100);btnLogin.setPrefHeight(60);
        HBox hboxButtons = new HBox(btnRegister, btnLogin);
        hboxButtons.setAlignment(Pos.CENTER);
        hboxButtons.setSpacing(20);
        vBox.getChildren().add(hboxButtons);
        VBox.setMargin(hboxButtons, new Insets(30,0,0,0));
        setCenter(hbox);
    }
    private void registerHandlers() {
        modelManager.addPropertyChangeListener(ModelManager.PROP_STATUS, (event) -> updateView() );
        btnLogin.setOnAction(actionEvent -> {
            String email = tfEmail.getText();
            String password = tfPassword.getText();

            modelManager.sendMessage(new ClientMSG());
        });
        btnRegister.setOnAction(actionEvent -> {
          modelManager.setStatusClient(StatusClient.REGISTER);
        });

    }

    private void updateView() {
        this.setVisible(modelManager != null && modelManager.getStatusClient() == StatusClient.NOT_LOGGED);
    }
}
