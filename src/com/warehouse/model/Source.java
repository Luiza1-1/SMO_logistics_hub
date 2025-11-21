package com.warehouse.model;

import com.warehouse.enums.CargoType;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import java.util.Random;

public class Source {
    private final int id;
    private final ExponentialDistribution expDistribution;
    private final Random random;
    private int requestCounter;
    private int rejectedCount;
    private double nextGenerationTime;

    public Source(int id, double lambda) {
        this.id = id;
        this.expDistribution = new ExponentialDistribution(1.0 / lambda);
        this.random = new Random();
        this.requestCounter = 0;
        this.rejectedCount = 0;
        this.nextGenerationTime = 0; // Начальное время = 0, заявок еще нет
    }

    public Request generateRequest(double currentTime) {
        requestCounter++;  // УВЕЛИЧИВАЕМ счетчик!

        // Обновляем время следующей генерации
        this.nextGenerationTime = currentTime + generateInterArrivalTime();

        CargoType cargoType = random.nextDouble() < 0.1 ? CargoType.PERISHABLE : CargoType.REGULAR;

        Request request = new Request(requestCounter, currentTime, cargoType, this.id);

        System.out.printf(">>> ИСТОЧНИК %d: Сгенерирована заявка %d (%s)%n",
                id, requestCounter, cargoType.getDescription());

        return request;
    }

    public double generateInterArrivalTime() {
        return expDistribution.sample();
    }

    // ДОБАВЛЕННЫЕ МЕТОДЫ
    public int getGeneratedCount() {
        return requestCounter;
    }

    public int getRejectedCount() {
        return rejectedCount;
    }

    public double getNextGenerationTime() {
        return nextGenerationTime;
    }

    public void setNextGenerationTime(double nextGenerationTime) {
        this.nextGenerationTime = nextGenerationTime;
    }

    public void incrementRejectedCount() {
        rejectedCount++;
    }

    public int getId() { return id; }
    public int getRequestCounter() { return requestCounter; }
}