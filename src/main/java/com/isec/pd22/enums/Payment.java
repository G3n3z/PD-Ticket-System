package com.isec.pd22.enums;

public enum Payment {
    NOT_PAYED, PAYED;


    public static Payment fromInteger(int x) {
        switch(x) {
            case 0:
                return NOT_PAYED;
            case 1:
                return PAYED;
        }
        return NOT_PAYED;
    }

    public static String fromString(Payment payment) {
        return switch (payment){
            case NOT_PAYED -> "Não Pago";
            case PAYED -> "Pago";
        };
    }
}
