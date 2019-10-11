package com.redhat.cajun.navy.responder.simulator.data;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.vertx.core.json.Json;

public class MissionCommand {

    private String id;
    private String messageType;
    private String invokingService;
    private long timestamp;
    private Body bodyObject;
    private Map<String, String> header = new HashMap<>();

    // Getter Methods


    public void createMissionCommandHeaders(String messageType, String invokingService, long timestamp) {
        this.id = UUID.randomUUID().toString();
        this.messageType = messageType;
        this.invokingService = invokingService;
        this.timestamp = timestamp;
    }

    public void createMissionCommandHeaders(String messageType){
        this.createMissionCommandHeaders(messageType, "MissionService", System.currentTimeMillis());
    }

    public void createId(){
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getInvokingService() {
        return invokingService;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Body getBody() {
        return bodyObject;
    }

    // Setter Methods

    public void setId( String id ) {
        this.id = id;
    }

    public void setMessageType( String messageType ) {
        this.messageType = messageType;
    }

    public void setInvokingService( String invokingService ) {
        this.invokingService = invokingService;
    }

    public void setTimestamp(long timestamp ) {
        this.timestamp = timestamp;
    }

    public void setBody( Body bodyObject ) {
        this.bodyObject = bodyObject;
    }

    public void setMission(Mission m){
        this.setBody(Json.decodeValue(m.toString(), Body.class));
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public void setHeader(Map<String, String> header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return Json.encode(this);
    }

}

