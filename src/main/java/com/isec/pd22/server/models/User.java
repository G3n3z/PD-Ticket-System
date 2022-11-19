package com.isec.pd22.server.models;

import com.isec.pd22.enums.Authenticated;
import com.isec.pd22.enums.Role;

import java.io.Serial;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    int idUser;
    Role role;
    String username;
    String nome;
    String password;
    Authenticated authenticated;

    public User() {
    }

    public User(int idUser, Role role, String username, String nome) {
        this.idUser = idUser;
        this.role = role;
        this.username = username;
        this.nome = nome;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static User mapToEntity(ResultSet res) throws SQLException {
        User u = new User();

        u.idUser = res.getInt("id");
        u.nome = res.getString("nome");
        u.username = res.getString("username");
        u.role = Role.fromInteger(res.getInt("administrador"));
        u.password = res.getString("password");
        u.authenticated = Authenticated.fromInteger(res.getInt("autenticado"));
        return u;
    }

    public Authenticated getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Authenticated authenticated) {
        this.authenticated = authenticated;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
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
}
