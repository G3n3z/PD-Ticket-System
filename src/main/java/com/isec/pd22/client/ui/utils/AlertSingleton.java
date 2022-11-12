package com.isec.pd22.client.ui.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class AlertSingleton extends Alert {

    private static AlertSingleton instance;
    private AlertSingleton(AlertType alertType, ButtonType ...b) {
        super(alertType, "", b);
    }

    public static AlertSingleton getInstanceWarning() {
        return instance = new AlertSingleton(AlertType.INFORMATION, ButtonType.OK);

    }
    public static AlertSingleton getInstanceConfirmation() {
        return instance = new AlertSingleton(AlertType.CONFIRMATION, ButtonType.YES, ButtonType.NO);
    }
    public static AlertSingleton getInstanceOK() {
        return instance = new AlertSingleton(AlertType.CONFIRMATION, ButtonType.OK);
    }


    public AlertSingleton setProgress(ProgressIndicator progress){
        instance.setGraphic(progress);
        return instance;
    }


    public int countOfLines(String text){
        int count = 1;
        for (char c : text.toCharArray()) {
            if(c =='\n'){
                count++;
            }
        }
        return count;
    }
    public AlertSingleton setAlertText(String title, String header, String context){
        this.setTitle(title);
        this.setHeaderText(header);
        this.setContentText(context);
        this.setResizable(true);
        if(!context.equals("")) {
            this.getDialogPane().setPrefSize(480, 150 + countOfLines(context) * 20);
        }else
            this.getDialogPane().setPrefSize(300, 150);
        return this;
    }

}
