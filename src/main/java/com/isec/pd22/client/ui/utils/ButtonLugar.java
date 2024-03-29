package com.isec.pd22.client.ui.utils;

import com.isec.pd22.enums.Payment;
import com.isec.pd22.server.models.Lugar;
import com.isec.pd22.server.models.Reserva;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class ButtonLugar extends Button {
    Lugar lugar;
    boolean isSelected;
    boolean isMarked;
    boolean isUser;
    boolean waitingPayment;
    public ButtonLugar(String text, Lugar lugar, Boolean isUser) {
        super(text);
        this.lugar = lugar;
        this.isSelected = true;
        isMarked = lugar.getReserva() != null;
        this.isUser = isUser;
        toogleStatus();
        this.setOnAction(actionEvent -> {
            toogleStatus();
        });
        this.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    }

    public ButtonLugar(String text, Lugar lugar) {
        super(text);
        this.lugar = lugar;
        this.isSelected = true;
        waitingPayment = calcWaitingPaymentFlag(lugar.getReserva());
        isMarked = calcPaymentFlag(lugar.getReserva());
        toogleStatus();
        this.setOnAction(actionEvent -> {
            toogleStatus();
        });
        this.setBorder(new Border(new BorderStroke(Color.BLACK,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    }

    private boolean calcPaymentFlag(Reserva reserva) {
        if (reserva == null)
            return false;
        return reserva.getPayment() == Payment.PAYED;
    }

    private boolean calcWaitingPaymentFlag(Reserva reserva) {
        if (reserva == null)
            return false;
        return reserva.getPayment() == Payment.NOT_PAYED;
    }

    public void toogleStatus(){
        if (waitingPayment){
            this.setBackground(new Background(new BackgroundFill(Color.rgb(223, 184, 27), CornerRadii.EMPTY, Insets.EMPTY)));
            return;
        }

        if(isMarked){
            this.setBackground(new Background(new BackgroundFill(Color.rgb(183, 36, 58), CornerRadii.EMPTY, Insets.EMPTY)));
            return;
        }
        isSelected = !isSelected;
        if(isSelected){
            this.setBackground(new Background(new BackgroundFill(Color.rgb(152, 179, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        }else{
            this.setBackground(new Background(new BackgroundFill(Color.rgb(228, 245, 246), CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }


    public Lugar getLugar() {
        return lugar;
    }

    public boolean isWaitingPayment() {
        return waitingPayment;
    }
}
