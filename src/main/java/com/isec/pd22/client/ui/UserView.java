package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.ButtonMenu;
import com.isec.pd22.client.ui.utils.MenuVertical;
import com.isec.pd22.client.ui.utils.TableEspetaculo;
import com.isec.pd22.client.ui.utils.TableReserva;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.server.models.Reserva;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class UserView extends BorderPane {

    ModelManager modelManager;

    MenuVertical menuVertical;
    ButtonMenu btnConsultaReservas, btnViewEspetaculos, btnLogout;
    TableEspetaculo espetaculoTableView;
    TableView<Reserva> reservaTableView;
    VBox vBox;
    Label title;

    public UserView(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
    }
    private void createViews() {
        prepareMenu();
        espetaculoTableView = new TableEspetaculo(modelManager,vBox, title, new ScrollPane());
        reservaTableView = new TableReserva(modelManager);
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

}
