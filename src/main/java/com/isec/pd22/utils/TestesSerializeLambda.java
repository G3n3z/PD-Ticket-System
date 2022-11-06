package com.isec.pd22.utils;

import java.util.function.Consumer;

public class TestesSerializeLambda {

    public static void main(String[] args) {
        Consumer<String> consumer = (abc) -> {
          System.out.println("abc");
        };
    }


}

class Threads extends  Thread{


    @Override
    public void run() {
        super.run();
    }
}
