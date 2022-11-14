package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.*;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
import com.isec.pd22.server.models.Lugar;
import com.isec.pd22.server.models.Reserva;
import com.isec.pd22.utils.Constants;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;

public class AdminView extends BorderPane {

    MenuVertical menuVertical;
    ButtonMenu btnConsultaReservas, btnConsultaReservasPayed, btnViewEspetaculos, btnInsertEspetaculo, btnLogout, btnEditUserInfo;

    ModelManager modelManager;
    TableEspetaculo espetaculoTableView;
    TableView<Reserva> reservaTableView;
    TableView<Reserva> reservaTableViewPayed;
    VBox vBox;
    ScrollPane scrollPane;
    EditView editView;
    Label title;
    List<ButtonLugar> buttons;
    FormFilters formFilters;
    AlertSingleton alert = null;

    SpectaculeDetails spectaculeDetails;
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
        editView = new EditView(modelManager);

    }

    private void createReservasTable() {
        reservaTableView = new TableReserva(modelManager, false);
        reservaTableViewPayed = new TableReserva(modelManager, true);

    }

    private void createTable() {
        espetaculoTableView = new TableEspetaculo(modelManager, vBox, title, scrollPane);
        formFilters = new FormFilters(modelManager);
    }

    private void prepareMenu() {
        btnConsultaReservas = new ButtonMenu("Reservas Nao Pagas");
        btnViewEspetaculos = new ButtonMenu("Espetaculos");
        btnInsertEspetaculo = new ButtonMenu("Inserir Espetaculo");
        btnConsultaReservasPayed = new ButtonMenu("Reservas Pagas");
        btnLogout = new ButtonMenu("Logout");
        btnEditUserInfo = new ButtonMenu("Editar Dados\nPessoais");
        menuVertical = new MenuVertical( btnViewEspetaculos, btnInsertEspetaculo,btnEditUserInfo, btnConsultaReservas
                ,btnConsultaReservasPayed, btnLogout);
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

        btnEditUserInfo.setOnAction(actionEvent -> {
            editView = new EditView(modelManager);
            vBox.getChildren().clear();
            vBox.getChildren().add(editView);
        });

        btnInsertEspetaculo.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Abrir ficheiro...");
            fileChooser.setInitialDirectory(new File(Constants.PICK_FILES_DIR_PATH));
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
        modelManager.addPropertyChangeListener(ModelManager.PROP_CLOSE_ALERT, evt -> Platform.runLater(this::closeAlert));
        modelManager.addPropertyChangeListener(ModelManager.PROP_ESPETACULO_DETAILS_WAITING_PAYMENT,
                evt -> Platform.runLater(this::waitingPayment));

        btnViewEspetaculos.setOnAction(actionEvent -> {
            goToSpectacles();
        });
        btnConsultaReservas.setOnAction(actionEvent -> {
            RequestListReservas request = new RequestListReservas(ClientActions.CONSULT_UNPAYED_RESERVATION);
            request.setUser(modelManager.getUser());
            modelManager.sendMessage(request);
            vBox.getChildren().clear();
            title.setText("Reservas Nao Pagas");
            vBox.getChildren().addAll(title,reservaTableView);
        });
        btnConsultaReservasPayed.setOnAction(actionEvent -> {
            RequestListReservas request = new RequestListReservas(ClientActions.CONSULT_PAYED_RESERVATION);
            request.setUser(modelManager.getUser());
            modelManager.sendMessage(request);
            vBox.getChildren().clear();
            title.setText("Reservas Pagas");
            vBox.getChildren().addAll(title,reservaTableViewPayed);
        });
        modelManager.addPropertyChangeListener(ModelManager.PROP_DELETED_SPECPTACLE, evt -> Platform.runLater(this::deletedSpectacle));
    }

    private void deletedSpectacle() {
        AlertSingleton.getInstanceWarning().setAlertText("Espetaculo Removido", "", "Espetaculo foi removido")
                .showAndWait().ifPresent(buttonType -> {
                    goToSpectacles();
                });
    }

    public void goToSpectacles(){
        Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
        espetaculos.setUser(modelManager.getUser());
        modelManager.sendMessage(espetaculos);
        vBox.getChildren().clear();
        title.setText("Espetaculos");
        vBox.getChildren().addAll(title,espetaculoTableView, formFilters);
    }

    private void closeAlert() {
        if (alert!= null){
            alert.close();
        }
    }

    private void waitingPayment() {
        alert = AlertSingleton.getInstanceConfirmation().setAlertText("Bilhetes Reservados", "",
                        "Bilhetes Reservados com sucesso. Tem 10 segundos para remover");

        final ClientActions[] actions = new ClientActions[1];
        actions[0] = null;
        alert.showAndWait().ifPresent( buttonType -> {
            if (buttonType == ButtonType.YES){
                actions[0] = ClientActions.PAY_RESERVATION;

            }
        });
        if(actions[0]!= null) {
            sendPaymentMessage(actions[0]);
        }
    }

    private void sendPaymentMessage(ClientActions action) {
        RequestListReservas msg = new RequestListReservas(action);
        msg.setUser(modelManager.getUser());
        List<Reserva> reservas = spectaculeDetails.getButtons().stream().filter(ButtonLugar::isWaitingPayment).map(ButtonLugar::getLugar)
                .filter(lugar -> lugar.getReserva() != null && lugar.getReserva().getIdUser() == modelManager.getUser().getIdUser())
                .map(Lugar::getReserva).toList();
        msg.setReservas(reservas);
        modelManager.sendMessage(msg);
    }

    private void updateDetails() {
        spectaculeDetails = new SpectaculeDetails(modelManager, buttons);
        vBox.getChildren().clear();
        vBox.getChildren().addAll(title,spectaculeDetails);

    }

    private void updateReservas() {
        reservaTableView.getItems().clear();
        reservaTableView.getItems().addAll(modelManager.getReservas());
        reservaTableViewPayed.getItems().clear();
        reservaTableViewPayed.getItems().addAll(modelManager.getReservasPayed());

    }

    private void updateTable() {
        espetaculoTableView.getItems().clear();
        espetaculoTableView.getItems().addAll(modelManager.getEspectaculos());
    }

    private void actionSucceded() {
        AlertSingleton.getInstanceOK().setAlertText("File upload", "", "Ficheiro uploaded")
                .showAndWait();
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
        menuVertical.getChildren().addAll(btnViewEspetaculos,btnEditUserInfo, btnConsultaReservas,btnConsultaReservasPayed, btnLogout);
        espetaculoTableView.removeButtonRemove();
    }

    private void updateMenuAdmin() {
        menuVertical.getChildren().clear();
        menuVertical.getChildren().addAll( btnViewEspetaculos, btnInsertEspetaculo,btnEditUserInfo, btnConsultaReservas,btnConsultaReservasPayed, btnLogout);
        espetaculoTableView.addButtonRemove();
    }


}
