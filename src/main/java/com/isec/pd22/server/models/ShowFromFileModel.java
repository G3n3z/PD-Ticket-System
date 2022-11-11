package com.isec.pd22.server.models;

import com.isec.pd22.utils.Constants;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class ShowFromFileModel {
    private static final String FIELD_NAME_DESCRIPTION = "Designação";
    private static final String FIELD_NAME_TYPE = "Tipo";
    private static final String FIELD_NAME_DATE = "Data";
    private static final String FIELD_NAME_HOUR = "Hora";
    private static final String FIELD_NAME_DURATION = "Duração";
    private static final String FIELD_NAME_LOCAL = "Local";
    private static final String FIELD_NAME_CITY = "Localidade";
    private static final String FIELD_NAME_COUNTRY = "País";
    private static final String FIELD_NAME_AGE_CLASSIFICATION = "Classificação etária";
    private static final String FIELD_NAME_QUEUE = "Fila";


    private final Espetaculo show = new Espetaculo();

    private final HashMap<String, Lugar[]> seatsMap = new HashMap<>();

    private final Date showDate = new Date();

    public ShowFromFileModel(final String filePath) throws Exception {
        BufferedReader imageReader = new BufferedReader(new FileReader(filePath));

        boolean isReadingQueuesData = false;

        while (true) {
            String line = imageReader.readLine();

            if (line == null) { break; }

            String[] fields = line.split(";");
            fields = removeUnnecessaryChars(fields);

            if (fields[0].equals(FIELD_NAME_QUEUE)) {
                isReadingQueuesData = true;
                continue;
            }

            if (!isReadingQueuesData) {
                collectHeaderData(fields);
            } else {
                collectQueuesData(fields);
            }
        }
    }

    private void collectHeaderData(String[] fields) {
        String field = fields[0].trim();
        String value = fields[1].trim();

        switch (field) {
            case FIELD_NAME_DESCRIPTION ->
                    show.setDescricao(value);
            case FIELD_NAME_TYPE ->
                    show.setTipo(value);
            case FIELD_NAME_DATE -> {
                showDate.setDate(Integer.parseInt(fields[1]));
                showDate.setMonth(Integer.parseInt(fields[2]));
                showDate.setYear(Integer.parseInt(fields[3]));
                show.setData_hora(showDate);
            }
            case FIELD_NAME_HOUR -> {
                showDate.setHours(Integer.parseInt(fields[1]));
                showDate.setMinutes(Integer.parseInt(fields[2]));
                show.setData_hora(showDate);
            }
            case FIELD_NAME_DURATION ->
                    show.setDuracao(Integer.parseInt(value));
            case FIELD_NAME_LOCAL ->
                    show.setLocal(value);
            case FIELD_NAME_CITY ->
                    show.setLocalidade(value);
            case FIELD_NAME_COUNTRY ->
                    show.setPais(value);
            case FIELD_NAME_AGE_CLASSIFICATION ->
                    show.setClassificacao_etaria(value);
            default -> throw new RuntimeException("O campo [" + field + "] não é conhecido. Abortar carregamento.");
        }
    }

    private void collectQueuesData(String[] fields) {
        String field = fields[0].trim();
        String value = fields[1].trim();

        if (seatsMap.containsKey(field)) {
            throw new RuntimeException("A fila ["+ field + "] já existe. Abortar carregamento.");
        };


    }

    private String[] removeUnnecessaryChars(String[] fields) {
        String[] fieldsCopy = fields.clone();

        for (int i = 0; i < fieldsCopy.length; i++) {
            fieldsCopy[i] = fieldsCopy[i].replaceAll("\"", "");
        }

        return fieldsCopy;
    }
}
