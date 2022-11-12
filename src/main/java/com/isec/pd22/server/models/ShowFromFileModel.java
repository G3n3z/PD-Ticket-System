package com.isec.pd22.server.models;

import com.isec.pd22.exception.ServerException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

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

    private final HashMap<String, List<Lugar>> seatsMap = new HashMap<>();

    private final Date showDate = new Date();

    public ShowFromFileModel(final String filePath) throws Exception {
        BufferedReader imageReader = new BufferedReader(new FileReader(filePath));

        boolean isReadingQueuesData = false;

        while (true) {
            String line = imageReader.readLine();

            if (line == null) { break; }

            String[] fields = line.split(";");
            fields = removeUnnecessaryChars(fields);

            if (line.equals("") || fields[0].equals(FIELD_NAME_QUEUE)) {
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

    public Espetaculo getShow() {
        return show;
    }

    public HashMap<String, List<Lugar>> getSeatsMap() {
        return seatsMap;
    }

    public List<Lugar> getSeats() {
        ArrayList<Lugar> seatsMerged = new ArrayList<>();
        for (List<Lugar> queue:
                this.seatsMap.values()) {
            seatsMerged.addAll(queue);
        }

        return seatsMerged;
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
            default -> throw new ServerException("O campo [" + field + "] não é conhecido. Abortar carregamento.");
        }
    }

    private void collectQueuesData(String[] fields) {
        String field = fields[0].trim();
        String value = fields[1].trim();

        if (seatsMap.containsKey(field)) {
            throw new ServerException("A fila ["+ field + "] já existe. Abortar carregamento.");
        };

        ArrayList<Lugar> seats = new ArrayList<>();

        for (int i = 1; i < fields.length; i++) {
            String[] seatData = fields[i].split(":");
            seats.add(new Lugar(field, seatData[0], Double.parseDouble(seatData[1])));
        }

        seatsMap.put(field, seats);
    }

    private String[] removeUnnecessaryChars(String[] fields) {
        String[] fieldsCopy = fields.clone();

        for (int i = 0; i < fieldsCopy.length; i++) {
            fieldsCopy[i] = fieldsCopy[i].replaceAll("\"", "");
        }

        return fieldsCopy;
    }
}
