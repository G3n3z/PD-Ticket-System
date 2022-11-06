package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.client.ui.utils.ButtonMenu;
import com.isec.pd22.client.ui.utils.MenuVertical;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.ClientMSG;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.Reserva;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class AdminView extends BorderPane {

    MenuVertical menuVertical;
    ButtonMenu btnConsultaReservas, btnViewEspetaculos, btnInsertEspetaculo, btnLogout;

    ModelManager modelManager;
    TableView<Espetaculo> espetaculoTableView;
    TableView<Reserva> reservaTableView;
    BorderPane center;
    VBox vBox;
    Label title;
    public AdminView(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
        updateView();
    }


    private void createViews() {
        prepareMenu();
        center = new BorderPane();
        createTable();
        createReservasTable();
        title = new Label("Espetaculos");
        title.setFont(new Font(20));
        title.setAlignment(Pos.CENTER);
        vBox = new VBox();
        vBox.getChildren().addAll(title, espetaculoTableView);
        vBox.setPrefWidth(1000);
        vBox.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(title, new Insets(30,0,30,0));
        setCenter(vBox);
    }

    private void createReservasTable() {
        reservaTableView = new TableView<>();

        TableColumn<Reserva, String> colDataHora = new TableColumn<>("Descrição");
        colDataHora.setCellValueFactory(new PropertyValueFactory<>("data_hora"));
        TableColumn<Reserva, Payment> columnTipo = new TableColumn<>("Pago");
        columnTipo.setCellValueFactory(new PropertyValueFactory<>("payment"));
        TableColumn<Reserva, Integer> colUser = new TableColumn<>("Utilizador");
        colUser.setCellValueFactory(new PropertyValueFactory<>("idUser"));
        TableColumn<Reserva, Integer> colEspetaculo = new TableColumn<>("Espetaculo");
        colEspetaculo.setCellValueFactory(new PropertyValueFactory<>("idEspectaculo"));
        espetaculoTableView.setFixedCellSize(50);
        reservaTableView.getColumns().addAll(colDataHora,columnTipo,colUser, colEspetaculo);
        espetaculoTableView.setPrefHeight(400);
        espetaculoTableView.setPrefWidth(1000);
    }

    private void createTable() {
        espetaculoTableView = new TableView<>();
        TableColumn<Espetaculo, String> colDescricao = new TableColumn<>("Descrição");
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        TableColumn<Espetaculo, String> columnTipo = new TableColumn<>("Tipo");
        columnTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        TableColumn<Espetaculo, Date> columnDate = new TableColumn<>("Data");
        columnDate.setCellValueFactory(new PropertyValueFactory<>("data_hora"));
        TableColumn<Espetaculo, Integer> columnDuracao = new TableColumn<>("Duração");
        columnDuracao.setCellValueFactory(new PropertyValueFactory<>("duracao"));
        TableColumn<Espetaculo, String> columnLocal = new TableColumn<>("Local");
        columnLocal.setCellValueFactory(new PropertyValueFactory<>("local"));
        TableColumn<Espetaculo, String> columnClassificacao = new TableColumn<>("Classifição");
        columnClassificacao.setCellValueFactory(new PropertyValueFactory<>("classificacao_etaria"));
        TableColumn<Espetaculo, Integer> columnVisivel = new TableColumn<>("visivel");
        columnVisivel.setCellValueFactory(new PropertyValueFactory<>("visivel"));
        TableColumn<Espetaculo, Button> colShow = new TableColumn<>("Ver");
        colShow.setCellValueFactory(alunoButtonCellDataFeatures -> {
            Button button = new Button("Remover");

            return new ReadOnlyObjectWrapper<>(button);
        });
        TableColumn<Espetaculo, Button> colResolve = new TableColumn<>("Remover");
        colResolve.setCellValueFactory(alunoButtonCellDataFeatures -> {
            Button button = new Button("Remover");

            return new ReadOnlyObjectWrapper<>(button);
        });
        espetaculoTableView.setFixedCellSize(50);
        espetaculoTableView.getColumns().addAll(colDescricao, columnTipo, columnDate, columnDuracao, columnLocal, columnClassificacao, columnVisivel,
                colShow, colResolve);
        espetaculoTableView.setPrefHeight(400);
        espetaculoTableView.setPrefWidth(1000);
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
        modelManager.addPropertyChangeListener(ModelManager.ALL_ESPETACULOS, evt -> Platform.runLater(this::updateTable));
        modelManager.addPropertyChangeListener(ModelManager.PROP_RESERVAS, evt -> Platform.runLater(this::updateReservas));

        btnViewEspetaculos.setOnAction(actionEvent -> {
            Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
            espetaculos.setUser(modelManager.getUser());
            modelManager.sendMessage(espetaculos);
        });
        btnConsultaReservas.setOnAction(actionEvent -> {
            RequestListReservas request = new RequestListReservas(ClientActions.GET_RESERVS);
            request.setUser(modelManager.getUser());
            modelManager.sendMessage(request);
        });
    }

    private void updateReservas() {
        reservaTableView.getItems().clear();
        reservaTableView.getItems().addAll(modelManager.getReservas());
    }

    private void updateTable() {
        espetaculoTableView.getItems().clear();
        espetaculoTableView.getItems().addAll(modelManager.getEspectaculos());
    }

    private void actionSucceded() {
        AlertSingleton.getInstanceOK().setAlertText("File upload", "", "Ficheiro uploaded");
        AlertSingleton.getInstanceOK().showAndWait().ifPresent( action -> modelManager.setStatusClient(StatusClient.NOT_LOGGED));
    }


    private void updateView() {
        this.setVisible(modelManager != null && modelManager.getStatusClient() == StatusClient.ADMIN);
        if(modelManager != null && modelManager.getStatusClient() == StatusClient.ADMIN){
            espetaculoTableView.getItems().clear();
            espetaculoTableView.getItems().addAll(modelManager.getEspectaculos());
            Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
            espetaculos.setUser(modelManager.getUser());
            modelManager.sendMessage(espetaculos);
        }
    }


}
