package com.warehouse.model;

import com.warehouse.enums.CargoType;
import com.warehouse.enums.RequestStatus;

public class Request {
    private static int nextId = 1;
    private final int id;
    private final int sourceId;
    private final double arrivalTime;
    private final CargoType cargoType;
    private RequestStatus status;
    private int bufferPosition;
    private double serviceStartTime;
    private double serviceEndTime;
    private Double deadline;

    public Request(int id, double arrivalTime, CargoType cargoType, int sourceId) {
        this.id = id;
        this.sourceId = sourceId;
        this.arrivalTime = arrivalTime;
        this.cargoType = cargoType;
        this.status = RequestStatus.ARRIVED;
        this.deadline = null;
    }

    public void setBufferDeadline() {
        this.deadline = arrivalTime + cargoType.getDeadlineMinutes();
    }

    public void clearDeadline() {
        this.deadline = null;
    }

    public boolean hasDeadline() { return deadline != null; }
    public boolean isDeadlineExceeded(double currentTime) {
        return deadline != null && currentTime > deadline;
    }
    public double getWaitingTime(double currentTime) { return currentTime - arrivalTime; }
    public double getRemainingTime(double currentTime) {
        return deadline != null ? Math.max(0, deadline - currentTime) : 0;
    }


    // Getters and setters
    public int getId() { return id; }
    public int getSourceId() { return sourceId; }
    public double getArrivalTime() { return arrivalTime; }
    public CargoType getCargoType() { return cargoType; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public int getBufferPosition() { return bufferPosition; }
    public void setBufferPosition(int bufferPosition) { this.bufferPosition = bufferPosition; }
    public double getServiceStartTime() { return serviceStartTime; }
    public void setServiceStartTime(double serviceStartTime) { this.serviceStartTime = serviceStartTime; }
    public double getServiceEndTime() { return serviceEndTime; }
    public void setServiceEndTime(double serviceEndTime) { this.serviceEndTime = serviceEndTime; }
    public Double getDeadline() { return deadline; }
}