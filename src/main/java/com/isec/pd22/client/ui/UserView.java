package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.*;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.server.models.Reserva;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class UserView extends BorderPane {

    ModelManager modelManager;

    MenuVertical menuVertical;
    ButtonMenu btnConsultaReservas, btnViewEspetaculos, btnLogout;
    TableEspetaculo espetaculoTableView;
    TableView<Reserva> reservaTableView;
    VBox vBox;
    Label title;
    List<ButtonLugar> buttons;

    public UserView(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
    }
    private void createViews() {
        prepareMenu();
        title = new Label("Espetaculos");
        espetaculoTableView = new TableEspetaculo(modelManager,vBox, title, new ScrollPane());
        reservaTableView = new TableReserva(modelManager);
        vBox.getChildren().addAll(title, espetaculoTableView);
        setCenter(vBox);
    }

    private void prepareMenu() {
        btnConsultaReservas = new ButtonMenu("Reservas");
        btnViewEspetaculos = new ButtonMenu("Espetaculos");
        btnLogout = new ButtonMenu("Logout");
        menuVertical = new MenuVertical(btnViewEspetaculos, btnConsultaReservas, btnLogout);
        setLeft(menuVertical);
    }

    private void registerHandlers() {
        modelManager.addPropertyChangeListener(ModelManager.PROP_STATUS, evt -> updateView());
    }

    private void updateView() {
        this.setVisible(modelManager != null && modelManager.getStatusClient() == StatusClient.USER);
        if(modelManager != null && modelManager.getStatusClient() == StatusClient.USER){
            espetaculoTableView.getItems().clear();
            espetaculoTableView.getItems().addAll(modelManager.getEspectaculos());
            //TODO:  Ver que mensagem enviar
            Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
            espetaculos.setUser(modelManager.getUser());
            modelManager.sendMessage(espetaculos);
        }
    }


    private void updateDetails() {
        SpectaculeDetails spectaculeDetails = new SpectaculeDetails(modelManager, buttons);
        vBox.getChildren().clear();
        vBox.getChildren().addAll(title,spectaculeDetails);

    }

    private void updateReservas() {
        reservaTableView.getItems().clear();
        reservaTableView.getItems().addAll(modelManager.getReservas());
    }

    private void updateTable() {
        espetaculoTableView.getItems().clear();
        espetaculoTableView.getItems().addAll(modelManager.getEspectaculos());
    }

}
