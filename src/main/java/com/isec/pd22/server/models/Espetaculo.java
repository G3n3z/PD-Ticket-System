package com.isec.pd22.server.models;

import com.isec.pd22.utils.Constants;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class Espetaculo implements Serializable {

    int idEspetaculo;
    String descricao;
    String tipo;
    Date data_hora;
    int duracao;
    String local;
    String localidade;
    String pais;
    String classificacao_etaria;
    int visivel;
    Set<Lugar> lugares;

    public Espetaculo() {
        lugares = new HashSet<>();
    }

    public static Espetaculo mapToEntity(ResultSet res) throws SQLException {
        Espetaculo espetaculo = new Espetaculo();
        try {
            espetaculo.idEspetaculo = res.getInt("id");
            espetaculo.descricao = res.getString("descricao");
            espetaculo.tipo = res.getString("tipo");
            espetaculo.data_hora = Constants.stringToDate(res.getString("data_hora"));
            espetaculo.duracao = res.getInt("duracao");
            espetaculo.local = res.getString("local");
            espetaculo.pais = res.getString("pais");
            espetaculo.classificacao_etaria = res.getString("classificacao_etaria");
            espetaculo.visivel = res.getInt("visivel");
        }catch (SQLException e){
            throw e;
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return espetaculo;
    }

    public int getIdEspetaculo() {
        return idEspetaculo;
    }

    public void setIdEspetaculo(int idEspetaculo) {
        this.idEspetaculo = idEspetaculo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Date getData_hora() {
        return data_hora;
    }

    public void setData_hora(Date data_hora) {
        this.data_hora = data_hora;
    }

    public int getDuracao() {
        return duracao;
    }

    public void setDuracao(int duracao) {
        this.duracao = duracao;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getLocalidade() {
        return localidade;
    }

    public void setLocalidade(String localidade) {
        this.localidade = localidade;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getClassificacao_etaria() {
        return classificacao_etaria;
    }

    public void setClassificacao_etaria(String classificacao_etaria) {
        this.classificacao_etaria = classificacao_etaria;
    }

    public int getVisivel() {
        return visivel;
    }

    public void setVisivel(int visivel) {
        this.visivel = visivel;
    }

    public Set<Lugar> getLugares() {
        return lugares;
    }

    public void setLugares(Set<Lugar> lugares) {
        this.lugares = lugares;
    }
}
