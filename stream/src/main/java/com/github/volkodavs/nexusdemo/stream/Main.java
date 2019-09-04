package com.github.volkodavs.nexusdemo.stream;

import java.util.Scanner;

import com.github.volkodavs.nexusdemo.api.Service;

public class Main {

    public static void main(String[] args) {
        Service service = new ServiceImpl();
        while (true) {
            System.out.println("Enter your username: ");
            Scanner scanner = new Scanner(System.in);
            String username = scanner.nextLine();
            String helloTo = service.sayHello(username);
            System.out.println(helloTo);
        }
    }
}
