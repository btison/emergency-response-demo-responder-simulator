package com.redhat.cajun.navy.responder.simulator.data;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Responder {

    private static Logger logger = LoggerFactory.getLogger(Responder.class);

    private String responderId = null;

    private String missionId = null;

    private String incidentId = null;

    @JsonIgnore
    private Deque<MissionStep> queue = null;

    private double currentLat;

    private double currentLon;

    private boolean isHuman = false;

    private boolean isContinue = true;

    private double distanceUnit;

    private Status status = Status.RECEIVED;

    public enum Status {
        RECEIVED("RECEIVED"),
        PREP("PREP"),
        READY("READY"),
        MOVING("MOVING"),
        STUCK("STUCK"),
        PICKEDUP("PICKEDUP"),
        DROPPED("DROPPED");

        private String actionType;

        Status(String actionType) {
            this.actionType = actionType;
        }

        public String getActionType() {
            return actionType;
        }
    }



    public Responder() {
        queue = new LinkedList<>();
    }


    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


    public boolean isContinue() {
        return isContinue;
    }

    public void setContinue(boolean aContinue) {
        isContinue = aContinue;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public Queue<MissionStep> getQueue() {
        return queue;
    }

    public void setQueue(Deque<MissionStep> queue) {
        this.queue = queue;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getResponderId() {
        return responderId;
    }

    public void setResponderId(String responderId) {
        this.responderId = responderId;
    }

    public void setDistanceUnit(double distanceUnit) {
        this.distanceUnit = distanceUnit;
    }

    public MissionStep peek(){
        return queue.peek();
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }

    public MissionStep nextLocation() {
        MissionStep step =  queue.poll();
        if (step != null) {
            currentLat = step.getLat();
            currentLon = step.getLon();
        }
        return step;
    }

    public void addNextLocation(MissionStep step) {
        if (queue != null) {
            queue.add(step);
        }
    }


    public void setLocation(MissionStep location) {
        addNextLocation(location);
    }

    public MissionStep getLocation() {

        return queue.peek();
    }

    public boolean isHuman() {
        return isHuman;
    }

    public void setHuman(boolean human) {
        isHuman = human;
    }

    public void setCurrentLat(double currentLat) {
        this.currentLat = currentLat;
    }

    public void setCurrentLon(double currentLon) {
        this.currentLon = currentLon;
    }

    public void calculateNextLocation() {
        DistanceHelper.Coordinate current = new DistanceHelper.Coordinate(currentLat, currentLon);
        logger.info("Current location: " + currentLat + "," + currentLon);
        MissionStep step = queue.peek();
        logger.info("Next location: " + step.getLat() + "," + step.getLon());
        DistanceHelper.Coordinate destination = new DistanceHelper.Coordinate(step.getLat(), step.getLon());
        double distance = DistanceHelper.calculateDistance(current, destination);
        double intermediateDistance = 0.0;
        logger.info("Distance to next location: " + distance + " meter");
        while (distance * 1.3 < distanceUnit) {
            step = queue.peek();
            if (step.isWayPoint() || step.isDestination()) {
                break;
            }
            step = queue.poll();
            current = new DistanceHelper.Coordinate(step.getLat(), step.getLon());
            intermediateDistance = distance;
            logger.info("Moving to next location: " + step.getLat() + "," + step.getLon());
            step = queue.peek();
            destination = new DistanceHelper.Coordinate(step.getLat(), step.getLon());
            double nextDistance = DistanceHelper.calculateDistance(current, destination);
            distance = distance + nextDistance;
            logger.info("Distance to next location: " + distance + " meter");
        }
        if (distance > distanceUnit * 1.3) {
            logger.info("Adding new intermediate step");
            DistanceHelper.Coordinate intermediateCoordinate = DistanceHelper.calculateIntermediateCoordinate(current, destination, distanceUnit - intermediateDistance);
            logger.info("New step : " + intermediateCoordinate.getLat() + "," + intermediateCoordinate.getLon());
            MissionStep intermediateStep = new MissionStep();
            intermediateStep.setLat(intermediateCoordinate.getLat());
            intermediateStep.setLon(intermediateCoordinate.getLon());
            intermediateStep.setDestination(false);
            intermediateStep.setWayPoint(false);
            queue.addFirst(intermediateStep);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Responder responder = (Responder) o;
        return Objects.equals(responderId, responder.responderId);
    }


    @Override
    public int hashCode() {
        return responderId.hashCode();
    }

    @Override
    public String toString() {
        return Json.encode(this);
    }
}
