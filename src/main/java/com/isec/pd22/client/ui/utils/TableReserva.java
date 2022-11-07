package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.server.models.Reserva;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TableReserva extends TableView<Reserva> {

    ModelManager modelManager;
    public TableReserva(ModelManager modelManager) {
        this.modelManager = modelManager;
        createTable();
    }

    private void createTable() {

        TableColumn<Reserva, String> colDataHora = new TableColumn<>("Descrição");
        colDataHora.setCellValueFactory(new PropertyValueFactory<>("data_hora"));
        TableColumn<Reserva, Payment> columnTipo = new TableColumn<>("Pago");
        columnTipo.setCellValueFactory(new PropertyValueFactory<>("payment"));
        TableColumn<Reserva, Integer> colUser = new TableColumn<>("Utilizador");
        colUser.setCellValueFactory(new PropertyValueFactory<>("idUser"));
        TableColumn<Reserva, Integer> colEspetaculo = new TableColumn<>("Espetaculo");
        colEspetaculo.setCellValueFactory(new PropertyValueFactory<>("idEspectaculo"));
        setFixedCellSize(50);
        getColumns().addAll(colDataHora,columnTipo,colUser, colEspetaculo);
        setPrefHeight(400);
        setPrefWidth(1000);
    }


}
