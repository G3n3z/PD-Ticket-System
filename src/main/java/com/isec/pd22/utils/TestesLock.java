package com.isec.pd22.utils;

import java.util.Scanner;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TestesLock {

    public static void main(String[] args) {
        Boolean bool = true;
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        //lock.lock();
        for (int i = 0; i < 4; i++) {
            ThreadLock threadLock = new ThreadLock(bool,condition,lock,i);
            threadLock.start();
        }
        Scanner sc = new Scanner(System.in);
        while (bool){
            System.out.println("Introduza: ");
            int number = sc.nextInt();

            switch (number){
                case 1 -> {
                    System.out.println("No 1");
                    lock.lock();
                    condition.signalAll();
                    lock.unlock();
                }
                case 2 -> {
                    bool = false;
                    lock.lock();
                    condition.signalAll();
                    lock.unlock();

                }
            }
        }
    }

}


class ThreadLock extends Thread{

    Boolean finish = false;
    Condition condition; Lock lock;
    Integer number;

    public ThreadLock(Boolean finish, Condition condition, Lock lock, Integer number) {
        this.finish = finish;
        this.condition = condition;
        this.lock = lock;
        this.number = number;
    }

    @Override
    public void run() {
        while (finish){
            System.out.println("Sou a thread " + number);
            try {
                lock.lock();
                condition.await();
                lock.unlock();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("A sair");
    }
}