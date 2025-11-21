package com.warehouse.utils;

import com.warehouse.model.Request;
import com.warehouse.enums.CargoType;
import java.util.*;

public class Statistics {
    private int totalArrivals;
    private int totalCompleted;
    private int totalRejected;
    private int totalEvicted;

    private final Map<CargoType, Integer> arrivalsByType;
    private final Map<CargoType, Integer> completedByType;
    private final Map<CargoType, Integer> rejectedByType;

    private final List<Double> waitingTimes;
    private final List<Double> serviceTimes;
    private final List<Double> systemTimes;

    public Statistics() {
        this.arrivalsByType = new EnumMap<>(CargoType.class);
        this.completedByType = new EnumMap<>(CargoType.class);
        this.rejectedByType = new EnumMap<>(CargoType.class);

        this.waitingTimes = new ArrayList<>();
        this.serviceTimes = new ArrayList<>();
        this.systemTimes = new ArrayList<>();

        // Инициализация счетчиков для всех типов грузов
        for (CargoType type : CargoType.values()) {
            arrivalsByType.put(type, 0);
            completedByType.put(type, 0);
            rejectedByType.put(type, 0);
        }
    }

    public void recordArrival(Request request) {
        totalArrivals++;
        arrivalsByType.merge(request.getCargoType(), 1, Integer::sum);
    }

    public void recordServiceCompletion(Request request, double currentTime) {
        totalCompleted++;
        completedByType.merge(request.getCargoType(), 1, Integer::sum);

        double systemTime = currentTime - request.getArrivalTime();
        systemTimes.add(systemTime);

        if (request.getServiceStartTime() > 0) {
            double serviceTime = currentTime - request.getServiceStartTime();
            serviceTimes.add(serviceTime);

            double waitingTime = request.getServiceStartTime() - request.getArrivalTime();
            waitingTimes.add(waitingTime);
        }
    }

    public void recordRejection(Request request) {
        totalRejected++;
        rejectedByType.merge(request.getCargoType(), 1, Integer::sum);
        System.out.printf(">>> ❌ СТАТИСТИКА: Заявка %d ОТКЛОНЕНА%n", request.getId());
    }

    public void recordEviction(Request request, double currentTime) {
        totalEvicted++;
        System.out.printf(">>> 🗑️ СТАТИСТИКА: Заявка %d ВЫТЕСНЕНА (время ожидания: %.2f мин)%n",
                request.getId(), request.getWaitingTime(currentTime));
    }

    public void displayCurrentStats() {
        printSubsection("📊 ТЕКУЩАЯ СТАТИСТИКА");

        System.out.printf("Всего заявок: %d%n", totalArrivals);
        System.out.printf("Обслужено: %d (%.1f%%)%n", totalCompleted, getCompletionRate() * 100);
        System.out.printf("Отказов: %d (%.1f%%)%n", totalRejected, getRejectionRate() * 100);
        System.out.printf("Вытеснено: %d%n", totalEvicted);

        printTimeStats();
    }

    public void generateReport() {
        printSection("📈 ФИНАЛЬНЫЙ ОТЧЕТ СИМУЛЯЦИИ");

        printGeneralStats();
        printCargoTypeStats();
        printTimeStats();
        printEfficiencyStats();
    }

    private void printGeneralStats() {
        printSubsection("📋 ОБЩАЯ СТАТИСТИКА");

        System.out.printf("Всего сгенерировано заявок: %d%n", totalArrivals);
        System.out.printf("Успешно обслужено: %d (%.1f%%)%n", totalCompleted, getCompletionRate() * 100);
        System.out.printf("Получили отказ: %d (%.1f%%)%n", totalRejected, getRejectionRate() * 100);
        System.out.printf("Вытеснено из буфера: %d%n", totalEvicted);
    }

    private void printCargoTypeStats() {
        printSubsection("🚚 СТАТИСТИКА ПО ТИПАМ ГРУЗОВ");

        for (CargoType type : CargoType.values()) {
            int arrivals = arrivalsByType.get(type);
            int completed = completedByType.get(type);
            int rejected = rejectedByType.get(type);

            double completionRate = calculateRate(completed, arrivals);
            double rejectionRate = calculateRate(rejected, arrivals);

            System.out.printf("%s:%n", type.getDescription());
            System.out.printf("  Прибыло: %d | Обслужено: %d (%.1f%%) | Отказов: %d (%.1f%%)%n",
                    arrivals, completed, completionRate * 100, rejected, rejectionRate * 100);
        }
    }

    private void printTimeStats() {
        if (waitingTimes.isEmpty() && serviceTimes.isEmpty() && systemTimes.isEmpty()) return;

        printSubsection("⏱️ ВРЕМЕННЫЕ ХАРАКТЕРИСТИКИ");

        if (!waitingTimes.isEmpty()) {
            System.out.printf("Среднее время ожидания: %.2f мин%n", getAverage(waitingTimes));
            System.out.printf("Макс. время ожидания: %.2f мин%n", getMax(waitingTimes));
        }
        if (!serviceTimes.isEmpty()) {
            System.out.printf("Среднее время обслуживания: %.2f мин%n", getAverage(serviceTimes));
            System.out.printf("Макс. время обслуживания: %.2f мин%n", getMax(serviceTimes));
        }
        if (!systemTimes.isEmpty()) {
            System.out.printf("Среднее время в системе: %.2f мин%n", getAverage(systemTimes));
            System.out.printf("Макс. время в системе: %.2f мин%n", getMax(systemTimes));
        }
    }

    private void printEfficiencyStats() {
        printSubsection("📈 ЭФФЕКТИВНОСТЬ СИСТЕМЫ");
        System.out.printf("Коэффициент загрузки системы: %.1f%%%n", getSystemLoad() * 100);
    }

    // Вспомогательные методы
    public double getCompletionRate() {
        return totalArrivals > 0 ? (double) totalCompleted / totalArrivals : 0;
    }

    public double getRejectionRate() {
        return totalArrivals > 0 ? (double) totalRejected / totalArrivals : 0;
    }


    private double calculateRate(int numerator, int denominator) {
        return denominator > 0 ? (double) numerator / denominator : 0;
    }

    private double getAverage(List<Double> times) {
        return times.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private double getMax(List<Double> times) {
        return times.stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    private double getSystemLoad() {
        return getCompletionRate();
    }

    private void printSection(String title) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println(title);
        System.out.println("=".repeat(80));
    }

    private void printSubsection(String title) {
        System.out.println("\n" + title);
        System.out.println("-".repeat(40));
    }

    // Getters
    public int getTotalArrivals() { return totalArrivals; }
    public int getTotalCompleted() { return totalCompleted; }
    public int getTotalRejected() { return totalRejected; }
}