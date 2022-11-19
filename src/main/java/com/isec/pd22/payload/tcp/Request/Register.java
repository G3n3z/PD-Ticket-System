package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.ClientMSG;

public class Register extends ClientMSG {

    String nome;
    String password;
    String userName;

    public Register(ClientActions action, String nome, String password, String userName) {
        super(action);
        this.nome = nome;
        this.userName = userName;
        this.password = password;
    }

    public String getNome() {
        return nome;
    }

    public void setEmail(String email) {
        this.nome = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
