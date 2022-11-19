package com.isec.pd22.enums;

public enum Authenticated {
    NOT_AUTHENTICATED, AUTHENTICATED;

    public static Authenticated fromInteger(int autenticado) {

        switch(autenticado) {
            case 0:
                return NOT_AUTHENTICATED;
            case 1:
                return AUTHENTICATED;
        }
        return NOT_AUTHENTICATED;

    }
}
