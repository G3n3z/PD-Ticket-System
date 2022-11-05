package com.isec.pd22.client.ui.utils;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class ButtonMenu extends Button {
    Color cor, corNotOver, corOver;
    Background bg, bgNotOver, bgOver;
    String text;
    public ButtonMenu(String text) {
        super(text);
        corNotOver =  Color.rgb(178, 176, 176);
        corOver =  Color.rgb(255,255,255);
        cor = corNotOver;
        bgNotOver = new Background(new BackgroundFill(Color.rgb(84,84,84), CornerRadii.EMPTY, Insets.EMPTY));
        bgOver = new Background(new BackgroundFill(Color.rgb(126,126,126), CornerRadii.EMPTY, Insets.EMPTY));
        bg = bgNotOver;
        createViews();
        registerHandler();
        update();
    }
    private void createViews() {
        this.setBackground(bgNotOver);
        this.setTextFill(cor);
        //this.setPrefWidth(Double.MAX_VALUE);
         setCursor(Cursor.HAND);
        setFont(new Font(14));
    }
    private void registerHandler() {
        this.setOnMouseEntered(mouseEvent -> {
            cor = corOver;
            bg = bgOver;
            update();
        });
        this.setOnMouseExited(mouseEvent -> {
            cor = corNotOver;
            bg = bgNotOver;
            update();
        });
    }

    private void update(){
        this.setTextFill(cor);
        this.setBackground(bg);
    }


}
