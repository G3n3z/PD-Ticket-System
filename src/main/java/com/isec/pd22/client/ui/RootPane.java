package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.enums.StatusClient;
import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

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
