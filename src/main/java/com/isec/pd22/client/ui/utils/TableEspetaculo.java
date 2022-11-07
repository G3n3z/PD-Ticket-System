package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.RequestDetailsEspetaculo;
import com.isec.pd22.server.models.Espetaculo;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TableEspetaculo extends TableView<Espetaculo> {

    ModelManager modelManager;
    VBox vBox;
    Label title;
    ScrollPane scrollPane;

    public TableEspetaculo(ModelManager modelManager) {
        this.modelManager = modelManager;
        createTable();
    }

    public TableEspetaculo(ModelManager modelManager, VBox vBox, Label title, ScrollPane scrollPane) {
        this.modelManager = modelManager;
        this.vBox = vBox;
        this.title = title;
        this.scrollPane = scrollPane;
        createTable();
    }

    private void createTable() {
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
                //vBox.getChildren().addAll(title, scrollPane);
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
        setFixedCellSize(50);
        getColumns().addAll(colDescricao, columnTipo, columnDate, columnDuracao, columnLocal, columnClassificacao, columnVisivel,
                colShow, colResolve);
        setPrefHeight(400);
        setPrefWidth(1000);
    }
}
