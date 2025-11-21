package com.warehouse;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("СИМУЛЯЦИЯ СКЛАДСКОГО ХАБА ПРОДОВОЛЬСТВЕННЫХ ТОВАРОВ");
        System.out.println("=".repeat(60));

        Scanner scanner = new Scanner(System.in);

        System.out.println("Выберите режим:");
        System.out.println("1 - ПОШАГОВЫЙ режим (с выводом в консоль)");
        System.out.println("2 - АВТОМАТИЧЕСКИЙ режим");
        System.out.print("Ваш выбор: ");

        int choice = scanner.nextInt();
        scanner.nextLine();

        Simulation simulation = new Simulation();

        if (choice == 1) {
            simulation.runStepByStep(scanner);
        } else {
            System.out.println("\n⚡ ЗАПУСК АВТОМАТИЧЕСКОГО РЕЖИМА");
            // Для автоматического режима генерируем заявки на всех источниках
            simulation.run(24 * 60);
            simulation.generateReport();
        }

        scanner.close();
    }
}