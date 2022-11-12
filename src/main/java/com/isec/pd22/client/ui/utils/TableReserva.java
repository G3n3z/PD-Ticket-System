package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.enums.Role;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
import com.isec.pd22.server.models.Reserva;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.ArrayList;
import java.util.List;

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
        TableColumn<Reserva, Button> colRemove = new TableColumn<>("Remove");
        colRemove.setCellValueFactory(reservaButtonCellDataFeatures ->  {
            Button button = new Button("Remover");
            button.setOnAction(actionEvent -> {
                RequestListReservas listReservas = new RequestListReservas(ClientActions.CANCEL_RESERVATION, List.of(reservaButtonCellDataFeatures.getValue()));
                listReservas.setUser(modelManager.getUser());
                modelManager.sendMessage(listReservas);
            });
            return new ReadOnlyObjectWrapper<>(button);
        });
        TableColumn<Reserva, Button> colPay = new TableColumn<>("Espetaculo");
        colPay.setCellValueFactory(reservaButtonCellDataFeatures ->  {
            Button button = new Button("Pagar");
            button.setOnAction(actionEvent -> {
                RequestListReservas listReservas = new RequestListReservas(ClientActions.CANCEL_RESERVATION, List.of(reservaButtonCellDataFeatures.getValue()));
                listReservas.setUser(modelManager.getUser());
                modelManager.sendMessage(listReservas);
            });
            return new ReadOnlyObjectWrapper<>(button);
        });
        setFixedCellSize(50);
        getColumns().addAll(colDataHora,columnTipo,colUser, colEspetaculo);
        if (modelManager.getUser().getRole() == Role.ADMIN){
            getColumns().add(colRemove);
        }

        setPrefHeight(400);
        setPrefWidth(1000);
    }


}
