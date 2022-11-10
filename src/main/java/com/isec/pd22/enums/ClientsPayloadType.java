package com.isec.pd22.enums;

public enum ClientsPayloadType {
    REQUEST_SERVERS,
    REQUEST_SERVERS_SUCCESS,
    CONNECTION_LOST,
    BAD_REQUEST,
    LOGGED_IN,
    /**
     * Quando se nao recebes os prepares, ou seja  quando é lançado o abort
     */
    TRY_LATER,
    LOGOUT,
    /**
     * Ficheiro atualizado
     */
    FILE_UPDATED,
    PART_OF_FILE_UPLOADED,
    CONSULT_SPECTACLE,
    RESERVAS_RESPONSE,
    SPECTACLE_DETAILS,
    USER_REGISTER,
    ACTION_SUCCEDED

}
