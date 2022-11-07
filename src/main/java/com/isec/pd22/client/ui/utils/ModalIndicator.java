package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import javafx.stage.Popup;


public class ModalIndicator extends Popup {
    ProgressIndicator indicator;
    ModelManager modelManager;

    public ModalIndicator(ModelManager modelManager) {
        this.modelManager = modelManager;
        indicator = new ProgressIndicator();
        show();
    }

    public void stop(){
        indicator.stop();
        hide();
    }
}
