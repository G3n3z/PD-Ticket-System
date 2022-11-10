package com.isec.pd22.client.ui;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class EditView extends BorderPane {

    ModelManager modelManager;
    HBox hbox;
    VBox vBox;
    Label labelUsername, labelName, labelPassword;
    TextField tfUsername, tfName, tfPassword;
    Button btnSubmit, btnBack;

    public EditView(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
        updateView();
    }


    private void createViews() {
        hbox = new HBox();
        vBox = new VBox();
        Label title = new Label("Editar Dados Pessoais");
        title.setFont(new Font(30));
        HBox hBoxTitle = new HBox(title);
        hBoxTitle.setAlignment(Pos.CENTER);
        HBox.setMargin(title, new Insets(0,0,50,0));
        labelUsername = new Label("Novo Username");
        labelUsername.setFont(new Font(20));
        tfUsername = new TextField(modelManager.getUser().getUsername());
        tfUsername.setPrefHeight(30);
        tfUsername.setPrefWidth(200);
        labelName = new Label("Novo Nome");
        labelName.setFont(new Font(20));
        tfName = new TextField(modelManager.getUser().getNome());
        tfName.setPrefHeight(30);
        tfName.setPrefWidth(200);
        labelPassword = new Label("Nova Passord");
        labelPassword.setFont(new Font(20));
        tfPassword = new TextField(modelManager.getUser().getPassword());
        tfPassword.setPrefHeight(30);
        tfPassword.setPrefWidth(200);
        vBox.getChildren().addAll(hBoxTitle, labelUsername, tfUsername, labelName, tfName, labelPassword, tfPassword);
        vBox.setAlignment(Pos.CENTER);
        vBox.setSpacing(20);
        vBox.setMinWidth(500);
        VBox.setMargin(tfUsername, new Insets(5,0,30,0));
        hbox.getChildren().addAll(vBox);
        hbox.setAlignment(Pos.CENTER);
        btnSubmit = new Button("Submeter");
        btnBack = new Button("Voltar");
        btnBack.setPrefWidth(100);
        btnBack.setPrefHeight(40);
        btnSubmit.setPrefWidth(100);
        btnSubmit.setPrefHeight(40);
        HBox hboxButtons = new HBox(btnBack, btnSubmit);
        hboxButtons.setAlignment(Pos.CENTER);
        hboxButtons.setSpacing(20);
        vBox.getChildren().add(hboxButtons);
        VBox.setMargin(hboxButtons, new Insets(30,0,0,0));
        setCenter(hbox);
    }
    private void registerHandlers() {
        modelManager.addPropertyChangeListener(ModelManager.PROP_STATUS, (event) -> updateView() );
        btnSubmit.setOnAction(actionEvent -> {
            String username = tfUsername.getText();
            String nome = tfName.getText();
            String password = tfPassword.getText();
            ClientMSG msg = new ClientMSG(ClientActions.EDIT_USER);
            User user = new User();
            user.setUsername(username);
            user.setNome(nome);
            user.setPassword(password);
            msg.setUser(user);
            modelManager.sendMessage(msg);
        });
        btnBack.setOnAction(actionEvent -> {
            this.setVisible(false);
        });


    }

    private void updateView() {

    }
}
