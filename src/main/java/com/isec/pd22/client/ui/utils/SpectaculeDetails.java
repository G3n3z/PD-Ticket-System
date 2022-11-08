package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Payment;
import com.isec.pd22.enums.StatusClient;
import com.isec.pd22.payload.tcp.Request.ListPlaces;
import com.isec.pd22.server.models.Espetaculo;
import com.isec.pd22.server.models.Lugar;
import com.isec.pd22.server.models.Reserva;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.*;

public class SpectaculeDetails extends ScrollPane {

    ModelManager manager;
    List<ButtonLugar> buttons;
    Button clean;
    Button submit;
    public SpectaculeDetails(ModelManager manager, List<ButtonLugar> buttonLugars) {
        this.manager = manager;
        this.buttons = new ArrayList<>();
        createView();
        registerHandlers();

    }

    private void registerHandlers() {
        clean.setOnAction(actionEvent -> {
            cleanButtons();
        });
        submit.setOnAction(actionEvent -> {
            submitReserv();
        });
    }

    private void submitReserv() {
        List<Lugar> lugarsToSubmit = new ArrayList<>();
        Reserva reserva = new Reserva(
                manager.getEspectaculo().getData_hora(),
                Payment.NOT_PAYED,
                manager.getUser().getIdUser(),
                manager.getEspectaculo().getIdEspetaculo()
        );
        buttons.forEach(buttonLugar -> {
            if (buttonLugar.isSelected && !buttonLugar.isMarked){
                buttonLugar.lugar.setReserva(reserva);
                lugarsToSubmit.add(buttonLugar.lugar);
            }
        });
        ListPlaces request = new ListPlaces();
        request.setAction(ClientActions.SUBMIT_RESERVATION);
        request.setPlaces(lugarsToSubmit);
        request.setUser(manager.getUser());
        manager.sendMessage(request);
    }

    private void cleanButtons() {
        buttons.forEach(buttonLugar -> {
            if (buttonLugar.isSelected){
                buttonLugar.toogleStatus();
            }
        });

    }

    private void createView() {
        VBox vBox1 = new VBox();
        Espetaculo espetaculo = manager.getEspectaculo();
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

        HBox hBoxButtons = prepareButtons();
        VBox vBox3;
        if (manager.getStatusClient() == StatusClient.USER){
            vBox3 = new VBox(vBox1, vBox2, hBoxButtons);
        }else {
            vBox3 = new VBox(vBox1, vBox2);
        }
        VBox.setMargin(vBox2, new Insets(100, 0, 50,0));

        setContent(vBox3);
        setFitToWidth(true);
    }

    private HBox prepareButtons() {
        HBox hBox = new HBox();
        clean = new Button("Limpar");
        clean.setPrefWidth(80); clean.setPrefHeight(30);
        submit = new Button("Submeter");
        submit.setPrefWidth(80); submit.setPrefHeight(30);
        if (manager.getStatusClient() == StatusClient.USER){
            hBox.getChildren().addAll(clean, submit);
        }else{
            hBox.getChildren().addAll(submit);
        }
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(20);
        return hBox;
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

            List<ButtonLugar> buttons = lugares.stream().map(lugar -> new ButtonLugar(lugar.getAssento()+":" + lugar.getPreco() + "€", lugar,
                    manager.getStatusClient() == StatusClient.USER)).toList();
            HBox hBox = new HBox(); hBox.getChildren().add(label);  hBox.getChildren().addAll(buttons);
            this.buttons.addAll(buttons);
            hBox.setSpacing(10);
            hBox.getChildren().forEach(n -> {
                if(n instanceof ButtonLugar){
                    ((ButtonLugar) n).setPrefWidth(70);
                }else if(n instanceof Label l){
                    l.setPrefWidth(20);
                    l.setFont(new Font(15));
                }
            });
            hBoxes.add(hBox);
        }

        vBox1.getChildren().addAll(hBoxes);
        vBox1.setSpacing(10);
        vBox1.setAlignment(Pos.CENTER);
        return vBox1;
    }


}
