package com.isec.pd22.enums;

public enum Role {
    USER,ADMIN;

    public static Role fromInteger(int x) {
        switch(x) {
            case 0:
                return USER;
            case 1:
                return ADMIN;
        }
        return USER;
    }
}
