package com.isec.pd22.payload;

import com.isec.pd22.enums.ClientActions;
import com.isec.pd22.enums.Role;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ClientMSG implements Serializable {
    ClientActions action;
    Role role;
    String username;
    String nome;
    String password;
    Spectacle spectacle;

    private Set<HeartBeat> serverList;

    public ClientMSG() {
        this.serverList = new HashSet<>();
    }

    public ClientActions getAction() {
        return action;
    }

    public void setAction(ClientActions action) {
        this.action = action;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public void setServerList(Set<HeartBeat> serverList) {
        this.serverList = serverList;
    }
}
