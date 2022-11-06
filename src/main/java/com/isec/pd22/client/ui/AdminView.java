package com.isec.pd22.client.ui;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.client.ui.utils.AlertSingleton;
import com.isec.pd22.client.ui.utils.ButtonLugar;
import com.isec.pd22.client.ui.utils.ButtonMenu;
import com.isec.pd22.client.ui.utils.MenuVertical;
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
    ButtonMenu btnConsultaReservas, btnViewEspetaculos, btnInsertEspetaculo, btnLogout;

    ModelManager modelManager;
    TableView<Espetaculo> espetaculoTableView;
    TableView<Reserva> reservaTableView;
    BorderPane center;
    VBox vBox;
    ScrollPane scrollPane;
    Label title;
    List<ButtonLugar> buttons;
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
        scrollPane = new ScrollPane();
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
        colShow.setCellValueFactory(espectaculoButtonCellDataFeatures -> {
            Button button = new Button("Ver Detalhes");
            button.setOnAction(actionEvent -> {
                RequestDetailsEspetaculo request = new RequestDetailsEspetaculo(ClientActions.CONSULT_SPECTACLE_DETAILS);
                request.setEspetaculo(espectaculoButtonCellDataFeatures.getValue());
                request.setUser(modelManager.getUser());
                modelManager.sendMessage(request);
                vBox.getChildren().clear();
                vBox.getChildren().addAll(title, scrollPane);
            });
            return new ReadOnlyObjectWrapper<>(button);
        });
        TableColumn<Espetaculo, Button> colResolve = new TableColumn<>("Remover");
        colResolve.setCellValueFactory(espetaculoButtonCellDataFeatures ->  {
            Button button = new Button("Remover");
            button.setOnAction(actionEvent -> {
                Espetaculos espetaculos = new Espetaculos(ClientActions.DELETE_SPECTACLE);
                espetaculos.setEspetaculos(new ArrayList<>( List.of(espetaculoButtonCellDataFeatures.getValue())));
                espetaculos.setUser(modelManager.getUser());
                modelManager.sendMessage(espetaculos);
            });
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
        modelManager.addPropertyChangeListener(ModelManager.PROP_ESPETACULO_DETAILS, evt -> Platform.runLater(this::updateDetails));

        btnViewEspetaculos.setOnAction(actionEvent -> {
            Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
            espetaculos.setUser(modelManager.getUser());
            modelManager.sendMessage(espetaculos);
            vBox.getChildren().clear();
            title.setText("Espetaculos");
            vBox.getChildren().addAll(title,espetaculoTableView);
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
        VBox vBox1 = new VBox();
        Espetaculo espetaculo = modelManager.getEspectaculo();
        Label desc = new Label("Descrição: " + espetaculo.getDescricao()); desc.setFont(new Font(20));
        Label tipo = new Label("Tipo: " + espetaculo.getTipo()); tipo.setFont(new Font(20));
        Label data = new Label("Data: " + espetaculo.getData_hora()); data.setFont(new Font(20));
        Label duracao = new Label("Duração: " + espetaculo.getDuracao()); duracao.setFont(new Font(20));
        Label local = new Label("Local: " + espetaculo.getLocal()); local.setFont(new Font(20));
        Label localidade = new Label("Localidade: " + espetaculo.getLocalidade()); localidade.setFont(new Font(20));
        Label classificacao_etaria = new Label("Classificação etaria: " + espetaculo.getPais()); classificacao_etaria.setFont(new Font(20));
        Label visivel = new Label("Pais " + espetaculo.getPais()); visivel.setFont(new Font(20));
        HBox hBox1 = new HBox(desc, tipo); hBox1.setSpacing(30); hBox1.setAlignment(Pos.CENTER);
        HBox hBox2 = new HBox(data, duracao); hBox2.setSpacing(30); hBox2.setAlignment(Pos.CENTER);
        HBox hBox3 = new HBox(local, localidade); hBox3.setSpacing(30); hBox3.setAlignment(Pos.CENTER);
        HBox hBox4 = new HBox(classificacao_etaria, visivel); hBox4.setSpacing(30); hBox4.setAlignment(Pos.CENTER);
        vBox1.getChildren().addAll(hBox1,hBox2,hBox3, hBox4);
        vBox1.setSpacing(20);
        vBox1.setAlignment(Pos.TOP_CENTER);
        VBox.setMargin(hBox1, new Insets(20,0,0,0));
        VBox vBox2 = preparaLugares(espetaculo);
        VBox vBox3 = new VBox(vBox1, vBox2);
        scrollPane = new ScrollPane(vBox3);
        scrollPane.setFitToWidth(true);
        vBox.getChildren().clear();
        vBox.getChildren().addAll(scrollPane);


    }

    private VBox preparaLugares(Espetaculo espetaculo) {
        VBox vBox1 = new VBox();
        Map<String, Set<Lugar>> lugaresByFila = new HashMap<>();
        for (Lugar lugar : espetaculo.getLugares()) {
            Set<Lugar> list = lugaresByFila.get(lugar.getFila());
            if(list != null){
                list.add(lugar);
            }else{
                list = new HashSet<>();
                list.add(lugar);
                lugaresByFila.put(lugar.getFila(), list);
            }
        }
        List<HBox> hBoxes = new ArrayList<>();

        for (Map.Entry<String, Set<Lugar>> entry : lugaresByFila.entrySet()){
            List<Lugar> lugares = new ArrayList<>(entry.getValue().stream().toList());
            Collections.sort(lugares);
            Label label = new Label(entry.getKey());

            buttons = lugares.stream().map(lugar -> new ButtonLugar(lugar.getAssento()+":" + lugar.getPreco(), lugar)).toList();
            HBox hBox = new HBox(); hBox.getChildren().add(label);  hBox.getChildren().addAll(buttons);
            hBox.getChildren().forEach(n -> {
                if(n instanceof ButtonLugar){
                    ((ButtonLugar) n).setPrefWidth(100);
                }else if(n instanceof Label l){
                    l.setPrefWidth(20);
                    l.setFont(new Font(15));
                }
            });
            hBoxes.add(hBox);
        }

        vBox1.getChildren().addAll(hBoxes);
        return vBox1;
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
