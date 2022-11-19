package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.ClientsPayloadType;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.RequestDetailsEspetaculo;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.beans.EventHandler;
import java.io.IOException;

public class RootPane extends BorderPane {
    ModelManager modelManager;
    Stage stage;

    String [] args;

    StackPane stack;
    Node node;

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
        stack = new StackPane();
        setCenter(stack);
        changeView();

    }
    public void changeView(){
        switch (modelManager.getStatusClient()){
            case NOT_LOGGED -> node = new LogInView(modelManager);
            case REGISTER -> node = new RegisterView(modelManager);
            default -> node = new AdminView(modelManager);
        }
        stack.getChildren().clear();
        stack.getChildren().add(node);
    }
    private void registerHandlers() {
        modelManager.addPropertyChangeListener(ModelManager.BAD_REQUEST, evt -> Platform.runLater( this::badRequest));
        modelManager.addPropertyChangeListener(ModelManager.LOGOUT, evt -> Platform.runLater(this::logout));
        modelManager.addPropertyChangeListener(ModelManager.ERROR_CONNECTION, (evt) -> Platform.runLater(this::showAlert));
        modelManager.addPropertyChangeListener(ModelManager.PROP_STATUS, (evt) -> Platform.runLater(this::changeView));
        modelManager.addPropertyChangeListener(ModelManager.PROP_TRY_LATER, evt -> Platform.runLater(this::tryLater));
        new Thread(){
            @Override
            public void run() {
                startServices();
            }
        }.start();
        stage.setOnCloseRequest(windowEvent -> {
            AlertSingleton.getInstanceConfirmation().setAlertText("Sair", "Pretende Sair da Aplicação?", "")
                    .showAndWait().ifPresent(result -> {
                if (result.getText().equalsIgnoreCase("YES")){
                    ClientMSG msg = new ClientMSG(ClientActions.EXIT);
                    msg.setUser(modelManager.getUser());
                    modelManager.sendMessage(msg);
                    modelManager.closeConnection();
                    Platform.exit();
                }
                else{
                    windowEvent.consume();
                }
            });
        });
    }

    private void tryLater() {
        AlertSingleton.getInstanceWarning().setAlertText("Ação indisponivel", "", "Não foi possivel efetuar a sua ação" +
                        " tente outra vez")
                .showAndWait();
    }

    private void showAlert(){
        AlertSingleton.getInstanceWarning().setAlertText("ERRO DE CONECAO", "", "Servidores nao disponiveis.\n " +
                "Por favor tente mais tarde")
                .showAndWait().ifPresent(result -> {
            if (result.getText().equalsIgnoreCase("OK")){
                Platform.exit();
            }
        });
    }

    private void logout() {
        AlertSingleton.getInstanceOK().setAlertText("Logout", "", "Obrigado e volte sempre")
                .showAndWait().ifPresent( action -> modelManager.setStatusClient(StatusClient.NOT_LOGGED));
    }


    private void badRequest() {
        ClientMSG lastMessage = modelManager.getLastMessage();

        if (lastMessage == null || lastMessage.getAction() == null ) {
            AlertSingleton.getInstanceWarning().setAlertText("Erro de Mensagem", "", "Não foi possivel fazer o pedido ao servidor")
                    .showAndWait();
            return;
        }

        String message = lastMessage.getMessage();

        if (lastMessage.getClientsPayloadType() == ClientsPayloadType.NOT_AUTHENTICATED ){
            AlertSingleton.getInstanceWarning().setAlertText("Erro de Mensagem", "","A sua sessão expirou")
                    .showAndWait();
            modelManager.setStatusClient(StatusClient.NOT_LOGGED);
            modelManager.clearData();
            return;
        }

        switch (lastMessage.getAction()) {
            // TODO tratar as diferentes mensagens
            case CONSULT_SPECTACLE_DETAILS -> {modelManager.fireEspectaculo(new RequestDetailsEspetaculo(ClientActions.CONSULT_SPECTACLE_DETAILS));}
            case CONSULT_SPECTACLE -> {
                if(node instanceof AdminView adminView){
                    adminView.goToSpectacles();
                }
            }
            case LOGIN -> {

            }
            default -> {}
        }

        AlertSingleton.getInstanceWarning().setAlertText("Erro de Mensagem", "", message)
                .showAndWait();
    }

}
