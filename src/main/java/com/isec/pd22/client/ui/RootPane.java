package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class RootPane extends BorderPane {
    ModelManager modelManager;
    Stage stage;

    String [] args;



    public RootPane(ModelManager modelManager, Stage stage, String [] args) {
        this.modelManager = modelManager;
        this.stage = stage;
        this.args = args;
        createViews();
        registerHandlers();
    }




    public void startServices() {
        modelManager.startServices(args);
        stage.setOnCloseRequest(windowEvent -> {
            AlertSingleton.getInstanceConfirmation().setAlertText("Sair", "Pretende Sair da Aplicação?", "");
            AlertSingleton.getInstanceConfirmation().showAndWait().ifPresent(result -> {
                if (result.getText().equalsIgnoreCase("YES")){
                    ClientMSG msg = new ClientMSG(ClientActions.EXIT);
                    msg.setUser(modelManager.getUser());
                    modelManager.sendMessage(msg);
                    Platform.exit();
                }
                else{
                    windowEvent.consume();
                }
            });
        });
    }

    private void createViews() {
        StackPane stack = new StackPane();
        LogInView logInView = new LogInView(modelManager);
        RegisterView registerView = new RegisterView(modelManager);
        AdminView adminView = new AdminView(modelManager);
        stack.getChildren().addAll(logInView, adminView, registerView);
        setCenter(stack);

    }
    private void registerHandlers() {
        modelManager.addPropertyChangeListener(ModelManager.BAD_REQUEST, evt -> Platform.runLater( this::badRequest));
        modelManager.addPropertyChangeListener(ModelManager.LOGOUT, evt -> Platform.runLater(this::logout));
        modelManager.addPropertyChangeListener(ModelManager.ERROR_CONNECTION, (evt) -> Platform.runLater(this::showAlert));
    }

    private void showAlert(){
        AlertSingleton.getInstanceWarning().setAlertText("ERRO DE CONECAO", "", "Servidores nao disponiveis.\n " +
                "Por favor tente mais tarde");

        AlertSingleton.getInstanceWarning().showAndWait().ifPresent(result -> {
            if (result.getText().equalsIgnoreCase("OK")){
                Platform.exit();
            }
        });
    }

    private void logout() {
        AlertSingleton.getInstanceOK().setAlertText("Logout", "", "Obrigado e volte sempre");
        AlertSingleton.getInstanceOK().showAndWait().ifPresent( action -> modelManager.setStatusClient(StatusClient.NOT_LOGGED));
    }


    private void badRequest() {
        AlertSingleton.getInstanceWarning().setAlertText("Erro de Mensagem", "", "Não foi possivel fazer o pedido ao servidor");
        AlertSingleton.getInstanceWarning().showAndWait();
    }

}
