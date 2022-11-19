package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.RequestDetailsEspetaculo;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.utils.Constants;
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
    TableColumn<Espetaculo, Button> colRemove, colShow, colSwitchVisible;
    TableColumn<Espetaculo, String> columnVisivel;
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
        TableColumn<Espetaculo, String> columnDate = new TableColumn<>("Data");
        columnDate.setCellValueFactory(espectaculoButtonCellDataFeatures ->
                new ReadOnlyObjectWrapper<>(Constants.dateToString(espectaculoButtonCellDataFeatures.getValue()
                        .getData_hora())));
        TableColumn<Espetaculo, Integer> columnDuracao = new TableColumn<>("Duração");
        columnDuracao.setCellValueFactory(new PropertyValueFactory<>("duracao"));
        TableColumn<Espetaculo, String> columnLocal = new TableColumn<>("Local");
        columnLocal.setCellValueFactory(new PropertyValueFactory<>("local"));
        TableColumn<Espetaculo, String> columnLocalidade = new TableColumn<>("Localidade");
        columnLocalidade.setCellValueFactory(new PropertyValueFactory<>("localidade"));
        TableColumn<Espetaculo, String> columnPais = new TableColumn<>("País");
        columnPais.setCellValueFactory(new PropertyValueFactory<>("pais"));
        TableColumn<Espetaculo, String> columnClassificacao = new TableColumn<>("Classifição");
        columnClassificacao.setCellValueFactory(new PropertyValueFactory<>("classificacao_etaria"));
        columnVisivel = new TableColumn<>("visivel");
        columnVisivel.setCellValueFactory(new PropertyValueFactory<>("visivel"));
        colShow = new TableColumn<>("Ver");
        colShow.setCellValueFactory(espectaculoButtonCellDataFeatures -> {
            Button button = new Button("Ver Detalhes");
            button.setOnAction(actionEvent -> {
                RequestDetailsEspetaculo request = new RequestDetailsEspetaculo(ClientActions.CONSULT_SPECTACLE_DETAILS);
                request.setEspetaculo(espectaculoButtonCellDataFeatures.getValue());
                request.setUser(modelManager.getUser());
                modelManager.sendMessage(request);
                //vBox.getChildren().clear();
                //vBox.getChildren().addAll(title, scrollPane);
            });
            return new ReadOnlyObjectWrapper<>(button);
        });
        colRemove = new TableColumn<>("Remover");
        colRemove.setCellValueFactory(espetaculoButtonCellDataFeatures ->  {
            Button button = new Button("Remover");
            button.setOnAction(actionEvent -> {
                RequestDetailsEspetaculo espetaculo = new RequestDetailsEspetaculo(ClientActions.DELETE_SPECTACLE);
                espetaculo.setEspetaculo(espetaculoButtonCellDataFeatures.getValue());
                espetaculo.setUser(modelManager.getUser());
                modelManager.sendMessage(espetaculo);
            });
            return new ReadOnlyObjectWrapper<>(button);
        });
        colSwitchVisible = new TableColumn<>("Visibilidade");
        colSwitchVisible.setCellValueFactory(espetaculoButtonCellDataFeatures ->  {
            Button button = new Button("Mudar Vis");
            button.setOnAction(actionEvent -> {
                RequestDetailsEspetaculo espetaculo = new RequestDetailsEspetaculo(ClientActions.SWITCH_VISIBILITY);
                espetaculo.setEspetaculo(espetaculoButtonCellDataFeatures.getValue());
                espetaculo.setUser(modelManager.getUser());
                modelManager.sendMessage(espetaculo);
            });
            return new ReadOnlyObjectWrapper<>(button);
        });
        setFixedCellSize(40);
        getColumns().addAll(colDescricao, columnTipo, columnDate, columnDuracao, columnLocal, columnLocalidade, columnPais, columnClassificacao);
        colDescricao.setPrefWidth(200);
        columnDate.setPrefWidth(150);
        colShow.setPrefWidth(90);
        colRemove.setPrefWidth(90);
        columnLocal.setPrefWidth(200);

        setPrefHeight(400);
        setPrefWidth(1000);
    }


    public void addButtonRemove(){
        if (!getColumns().contains(colRemove)){
            getColumns().remove(colShow);
            getColumns().add(columnVisivel);
            getColumns().add(colShow);
            getColumns().add(colSwitchVisible);
            getColumns().add(colRemove);
        }

    }

    public void removeButtonRemove(){
        if (!getColumns().contains(colShow)){
            getColumns().add(colShow);
        }
    }

}
