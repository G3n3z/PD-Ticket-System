package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.enums.Role;
import com.isec.pd22.payload.tcp.Request.RequestListReservas;
import com.isec.pd22.server.models.Reserva;
import com.isec.pd22.utils.Constants;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class TableReserva extends TableView<Reserva> {

    ModelManager modelManager;
    Boolean payed;
    public TableReserva(ModelManager modelManager, boolean b) {
        this.modelManager = modelManager;
        this.payed = b;
        createTable();
    }

    private void createTable() {

        TableColumn<Reserva, String> colId = new TableColumn<>("Id");
        colId.setCellValueFactory(new PropertyValueFactory<>("idReserva"));
        TableColumn<Reserva, String> colDataHora = new TableColumn<>("Data");
        colDataHora.setCellValueFactory( reservaStringCellDataFeatures ->
            new ReadOnlyObjectWrapper<>(Constants.dateToString(
                    reservaStringCellDataFeatures.getValue().getData_hora()
            ))
        );
        TableColumn<Reserva, String> columnTipo = new TableColumn<>("Estado");
        columnTipo.setCellValueFactory(reservaPaymentCellDataFeatures ->
                new ReadOnlyObjectWrapper<>(Payment.fromString(reservaPaymentCellDataFeatures.getValue().getPayment())));
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

        TableColumn<Reserva, Button> colPay = new TableColumn<>("Pagar");
        colPay.setCellValueFactory(reservaButtonCellDataFeatures ->  {
            Button button = new Button("Pagar");
            button.setOnAction(actionEvent -> {
                RequestListReservas listReservas = new RequestListReservas(ClientActions.PAY_RESERVATION, List.of(reservaButtonCellDataFeatures.getValue()));
                listReservas.setUser(modelManager.getUser());
                modelManager.sendMessage(listReservas);
            });
            return new ReadOnlyObjectWrapper<>(button);
        });
        setFixedCellSize(50);
        getColumns().addAll(colDataHora,columnTipo,colUser, colEspetaculo);
        if (modelManager.getUser().getRole() == Role.ADMIN && !payed){
            getColumns().add(colRemove);
        }
        if (!payed){
            getColumns().add(colPay);
        }
        colDataHora.setPrefWidth(150);
        columnTipo.setPrefWidth(100);
        setPrefHeight(400);
        setPrefWidth(1000);
    }


}
