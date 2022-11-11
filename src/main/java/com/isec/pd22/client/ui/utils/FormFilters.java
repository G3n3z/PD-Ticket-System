package com.isec.pd22.client.ui.utils;

import com.isec.pd22.client.models.ModelManager;
import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.Request.Espetaculos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class FormFilters extends VBox {

    ModelManager modelManager;
    HBox hBox1, hBox2, hBox3, hBox4, hboxBtns;
    Button submit;

    TextField tfDesc, tfTipo, tfData_Hora, tfDuracao, tfLocal, tfLocalidade, tfPais, tfClassificao;
    List<TextField> list;
    public FormFilters(ModelManager modelManager) {
        this.modelManager = modelManager;
        createViews();
        registerHandlers();
    }

    private void registerHandlers() {
        Espetaculos espetaculos = new Espetaculos(ClientActions.CONSULT_SPECTACLE);
        espetaculos.setUser(modelManager.getUser());

        submit.setOnAction(actionEvent -> {
            for (TextField textField : list) {
                if(textField.getText() != null && !textField.getText().isEmpty()){
                    espetaculos.getFiltros().put(textField.getPromptText().toLowerCase(), textField.getText());
                }
            }
        });
        modelManager.sendMessage(espetaculos);
    }

    private void createViews() {
        //Label label = new Label("Descricao");
        tfDesc = new TextField();
        tfDesc.setPromptText("Descricao");
        tfTipo = new TextField();
        tfTipo.setPromptText("Tipo");
        tfData_Hora = new TextField();
        tfData_Hora.setPromptText("Data_Hora");
        tfDuracao = new TextField();
        tfDuracao.setPromptText("Duracao");
        tfLocal = new TextField();
        tfLocal.setPromptText("Local");
        tfLocalidade = new TextField();
        tfLocalidade.setPromptText("Localidade");
        tfPais = new TextField();
        tfPais.setPromptText("Pais");
        tfClassificao = new TextField();
        tfClassificao.setPromptText("Classificacao");

        hBox1 = new HBox(tfDesc,tfTipo );
        hBox1.setSpacing(40);
        hBox1.setAlignment(Pos.CENTER);
        hBox2 = new HBox(tfData_Hora,tfDuracao );
        hBox2.setSpacing(40);
        hBox2.setAlignment(Pos.CENTER);
        hBox3 = new HBox(tfLocal,tfLocalidade );
        hBox3.setSpacing(40);
        hBox3.setAlignment(Pos.CENTER);
        hBox4 = new HBox(tfPais,tfClassificao );
        hBox4.setSpacing(40);
        hBox4.setAlignment(Pos.CENTER);
        submit = new Button("Filtrar");
        hboxBtns = new HBox(submit);
        hboxBtns.setAlignment(Pos.CENTER);
        list = new ArrayList<>();
        list.addAll(List.of(tfDesc, tfTipo, tfData_Hora, tfDuracao, tfLocal, tfLocalidade, tfPais, tfClassificao));
        getChildren().addAll(hBox1,hBox2,hBox3,hBox4, hboxBtns);
        setSpacing(30);
    }

}
