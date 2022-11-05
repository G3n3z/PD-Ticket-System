package com.isec.pd22.client.ui.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuVertical extends VBox {

    List<ButtonMenu> botoes;

    public MenuVertical(ButtonMenu ...buttons) {
        super();
        botoes = new ArrayList<>();
        botoes.addAll(Arrays.asList(buttons));
        createViews();
        registerHandler();
        update();
    }


    private void createViews() {
        getChildren().addAll(botoes);

        this.setWidth(200);
        for (ButtonMenu b : botoes){
            b.setPrefWidth(this.getWidth());
            b.setPrefHeight(50);
        }
        VBox.setMargin(botoes.get(0), new Insets(40,0,0,0));
        this.setBackground(new Background(new BackgroundFill(Color.rgb(84,84,84), CornerRadii.EMPTY, Insets.EMPTY)));
        setAlignment(Pos.TOP_CENTER);

    }
    private void registerHandler() {

    }

    private void update() {

    }

}
