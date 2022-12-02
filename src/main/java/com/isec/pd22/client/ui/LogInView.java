package com.isec.pd22.client.ui;

import com.isec.pd22.client.View;
import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.server.models.User;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

import java.beans.PropertyChangeListener;

public class LogInView extends BorderPane implements View {

    ModelManager modelManager;
    HBox hbox;
    VBox vBox;
    Label label, label2;
    TextField tfUsername, tfPassword;
    Button btnLogin, btnRegister;

    PropertyChangeListener callback;


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
        label = new Label("Username");
        label.setFont(new Font(20));
        tfUsername = new TextField();
        tfUsername.setPrefHeight(30);
        tfUsername.setPrefWidth(200);
        label2 = new Label("Password");
        label2.setFont(new Font(20));
        tfPassword = new TextField();
        tfPassword.setPrefHeight(30);
        tfPassword.setPrefWidth(200);
        vBox.getChildren().addAll(hBoxTitle,label, tfUsername, label2, tfPassword);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(20);
        vBox.setMinWidth(500);
        VBox.setMargin(tfUsername, new Insets(5,0,30,0));
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
        callback = (event) -> updateView();
        modelManager.addPropertyChangeListener(ModelManager.PROP_STATUS, callback );
        btnLogin.setOnAction(actionEvent -> {
            String username = tfUsername.getText();
            String password = tfPassword.getText();
            ClientMSG msg = new ClientMSG(ClientActions.LOGIN);
            msg.setUser(new User(username, password));
            modelManager.sendMessage(msg);
        });
        btnRegister.setOnAction(actionEvent -> {
          modelManager.setStatusClient(StatusClient.REGISTER);
        });


    }


    private void updateView() {
        this.setVisible(modelManager != null && modelManager.getStatusClient() == StatusClient.NOT_LOGGED);
        if(modelManager != null && modelManager.getStatusClient() == StatusClient.NOT_LOGGED)
            modelManager.clearData();
    }

    @Override
    public void removeListeners() {
        modelManager.removePropertyChangeListener(ModelManager.PROP_STATUS, callback );
    }
}
