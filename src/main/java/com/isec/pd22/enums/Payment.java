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
}
