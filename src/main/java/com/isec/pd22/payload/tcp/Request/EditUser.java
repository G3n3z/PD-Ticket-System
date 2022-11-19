package com.isec.pd22.payload.tcp.Request;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.payload.tcp.ClientMSG;

public class EditUser extends ClientMSG {
    String username;
    String nome;
    String password;

    public EditUser(ClientActions action, String username, String nome, String password) {
        super(action);
        this.username = username;
        this.nome = nome;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
