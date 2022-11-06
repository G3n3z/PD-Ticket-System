package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.ButtonMenu;
import com.isec.pd22.client.ui.utils.MenuVertical;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import javafx.scene.layout.BorderPane;

public class AdminView extends BorderPane {

    MenuVertical menuVertical;
    ButtonMenu btnConsultaReservas, btnViewEspetaculos, btnInsertEspetaculo, btnLogout;

    ModelManager modelManager;


    public AdminView(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
        updateView();
    }


    private void createViews() {
        prepareMenu();
    }

    private void prepareMenu() {
        btnConsultaReservas = new ButtonMenu("Reservas");
        btnViewEspetaculos = new ButtonMenu("Espetaculos");
        btnInsertEspetaculo = new ButtonMenu("Inserir Espetaculo");
        btnLogout = new ButtonMenu("Logout");
        menuVertical = new MenuVertical(btnViewEspetaculos, btnInsertEspetaculo, btnConsultaReservas, btnLogout);
        setLeft(menuVertical);
    }

    private void registerHandlers() {
        modelManager.addPropertyChangeListener(ModelManager.PROP_STATUS, evt -> updateView());
        btnLogout.setOnAction( evt -> {
            ClientMSG msg = new ClientMSG(ClientActions.LOGOUT);
            msg.setUser(modelManager.getUser());
            modelManager.sendMessage(msg);
        });
    }

    private void updateView() {
        this.setVisible(modelManager != null && modelManager.getStatusClient() == StatusClient.ADMIN);
    }


}
