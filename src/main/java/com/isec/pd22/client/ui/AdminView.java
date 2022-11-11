package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.*;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.RequestDetailsEspetaculo;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.Lugar;
import com.isec.pd22.server.models.Reserva;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

public class AdminView extends BorderPane {

    MenuVertical menuVertical;
    ButtonMenu btnConsultaReservas, btnViewEspetaculos, btnInsertEspetaculo, btnLogout, btnEditUserInfo;

    ModelManager modelManager;
    TableEspetaculo espetaculoTableView;
    TableView<Reserva> reservaTableView;
    TableView<Reserva> reservaTableViewPayed;
    VBox vBox;
    ScrollPane scrollPane;
    Label title;
    List<ButtonLugar> buttons;
    FormFilters formFilters;
    public AdminView(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
        updateView();
    }


    private void createViews() {
        prepareMenu();
        vBox = new VBox();
        createTable();
        createReservasTable();
        title = new Label("Espetaculos");
        title.setFont(new Font(20));
        title.setAlignment(Pos.CENTER);
        vBox.getChildren().addAll(title, espetaculoTableView, formFilters);
        vBox.setPrefWidth(1000);
        vBox.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(title, new Insets(30,0,30,0));
        VBox.setMargin(formFilters, new Insets(30,0,0,0));
        setCenter(vBox);
        scrollPane = new ScrollPane();
    }

    private void createReservasTable() {
        reservaTableView = new TableReserva(modelManager);
        if (modelManager.getStatusClient() == StatusClient.USER){
            reservaTableViewPayed = new TableReserva(modelManager);
        }

    }

    private void createTable() {
        espetaculoTableView = new TableEspetaculo(modelManager, vBox, title, scrollPane);
        formFilters = new FormFilters(modelManager);
    }

    private void prepareMenu() {
        btnConsultaReservas = new ButtonMenu("Reservas");
        btnViewEspetaculos = new ButtonMenu("Espetaculos");
        btnInsertEspetaculo = new ButtonMenu("Inserir Espetaculo");
        btnLogout = new ButtonMenu("Logout");
        btnEditUserInfo = new ButtonMenu("Editar Dados\nPessoais");
        menuVertical = new MenuVertical(btnEditUserInfo, btnViewEspetaculos, btnInsertEspetaculo, btnConsultaReservas, btnLogout);
        setLeft(menuVertical);
    }

    private void registerHandlers() {
        modelManager.addPropertyChangeListener(ModelManager.PROP_STATUS, evt ->
                Platform.runLater(this::updateView));

        btnLogout.setOnAction( evt -> {
            ClientMSG msg = new ClientMSG(ClientActions.LOGOUT);
            msg.setUser(modelManager.getUser());
            modelManager.sendMessage(msg);
        });

        btnInsertEspetaculo.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir ficheiro...");
            fileChooser.setInitialDirectory(new File("."));
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Ficheiro de texto (*.txt)", "*.txt")
            );
            File f = fileChooser.showOpenDialog(this.getScene().getWindow());
            if(f == null){
                return;
            }
            modelManager.sendFile(f);
        });

        modelManager.addPropertyChangeListener(ModelManager.FILE_UPDATED, evt -> Platform.runLater(this::actionSucceded));
        modelManager.addPropertyChangeListener(ModelManager.ALL_ESPETACULOS, evt -> Platform.runLater(this::updateTable));
        modelManager.addPropertyChangeListener(ModelManager.PROP_RESERVAS, evt -> Platform.runLater(this::updateReservas));
        modelManager.addPropertyChangeListener(ModelManager.PROP_ESPETACULO_DETAILS, evt -> Platform.runLater(this::updateDetails));

        btnViewEspetaculos.setOnAction(actionEvent -> {
            Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
            espetaculos.setUser(modelManager.getUser());
            modelManager.sendMessage(espetaculos);
            vBox.getChildren().clear();
            title.setText("Espetaculos");
            vBox.getChildren().addAll(title,espetaculoTableView, formFilters);
        });
        btnConsultaReservas.setOnAction(actionEvent -> {
            RequestListReservas request = new RequestListReservas(ClientActions.GET_RESERVS);
            request.setUser(modelManager.getUser());
            modelManager.sendMessage(request);
            vBox.getChildren().clear();
            title.setText("Reservas");
            vBox.getChildren().addAll(title,reservaTableView);
        });
    }

    private void updateDetails() {
        SpectaculeDetails spectaculeDetails = new SpectaculeDetails(modelManager, buttons);
        vBox.getChildren().clear();
        vBox.getChildren().addAll(title,spectaculeDetails);

    }

    private void updateReservas() {
        reservaTableView.getItems().clear();
        reservaTableView.getItems().addAll(modelManager.getReservas());
        if (reservaTableViewPayed != null){
            reservaTableViewPayed.getItems().clear();
            reservaTableViewPayed.getItems().addAll(modelManager.getReservasPayed());
        }
    }

    private void updateTable() {
        espetaculoTableView.getItems().clear();
        espetaculoTableView.getItems().addAll(modelManager.getEspectaculos());
    }

    private void actionSucceded() {
        AlertSingleton.getInstanceOK().setAlertText("File upload", "", "Ficheiro uploaded")
                .showAndWait().ifPresent( action -> modelManager.setStatusClient(StatusClient.NOT_LOGGED));
    }


    private void updateView() {
        this.setVisible(modelManager != null && (modelManager.getStatusClient() == StatusClient.ADMIN || modelManager.getStatusClient() == StatusClient.USER));
        if(modelManager != null && (modelManager.getStatusClient() == StatusClient.ADMIN || modelManager.getStatusClient() == StatusClient.USER)){
            espetaculoTableView.getItems().clear();
            Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
            espetaculos.setUser(modelManager.getUser());
            modelManager.sendMessage(espetaculos);

            espetaculoTableView.getItems().addAll(modelManager.getEspectaculos());
            reservaTableView.getItems().clear();
        }

        if (modelManager.getStatusClient() == StatusClient.ADMIN){
            updateMenuAdmin();

        }else if(modelManager.getStatusClient() == StatusClient.USER){
            updateMenuUser();
        }


    }

    private void updateMenuUser() {
        menuVertical.getChildren().clear();
        menuVertical.getChildren().addAll(btnEditUserInfo, btnViewEspetaculos, btnConsultaReservas, btnLogout);
        espetaculoTableView.removeButtonRemove();
    }

    private void updateMenuAdmin() {
        menuVertical.getChildren().clear();
        menuVertical.getChildren().addAll(btnEditUserInfo, btnViewEspetaculos, btnInsertEspetaculo, btnConsultaReservas, btnLogout);
        espetaculoTableView.addButtonRemove();
    }


}
