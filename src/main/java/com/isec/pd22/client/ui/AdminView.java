package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.client.ui.utils.ButtonMenu;
import com.isec.pd22.client.ui.utils.MenuVertical;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;

import java.io.File;

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
    }

    private void actionSucceded() {
        AlertSingleton.getInstanceOK().setAlertText("File upload", "", "Ficheiro uploaded");
        AlertSingleton.getInstanceOK().showAndWait().ifPresent( action -> modelManager.setStatusClient(StatusClient.NOT_LOGGED));
    }


    private void updateView() {
        this.setVisible(modelManager != null && modelManager.getStatusClient() == StatusClient.ADMIN);
    }


}
