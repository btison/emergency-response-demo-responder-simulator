package com.redhat.cajun.navy.responder.simulator;

import static com.redhat.cajun.navy.responder.simulator.EventConfig.RES_INQUEUE;
import static com.redhat.cajun.navy.responder.simulator.EventConfig.RES_OUTQUEUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.redhat.cajun.navy.responder.simulator.data.Mission;
import com.redhat.cajun.navy.responder.simulator.data.MissionCommand;
import com.redhat.cajun.navy.responder.simulator.data.Responder;
import io.reactivex.Single;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;


public class SimulationControl extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(SimulationControl.class);

    Set<Responder> responders = null;
    HashMap<String, Queue<Responder>> waitQueue = null;
    String uri = "/responder/";
    String host = "responder-service.naps-emergency-response.svc";
    int port = 8080;
    double distanceUnit;

    private int defaultTime = 5000;

    WebClient client = null;

    public enum MessageType {
        MissionStartedEvent("MissionStartedEvent"),
        MissionPickedUpEvent("MissionPickedUpEvent"),
        MissionCompletedEvent("MissionCompletedEvent");

        private String messageType;

        MessageType(String messageType) {
            this.messageType = messageType;
        }

        public String getMessageType() {
            return messageType;
        }

    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        responders = Collections.synchronizedSet(new HashSet<>(150));
        waitQueue = new HashMap<>(150);


        client = WebClient.create(vertx);
        host = config().getString("responder.service");
        uri = config().getString("responder.endpoint");
        port = config().getInteger("responder.port");
        distanceUnit = config().getDouble("simulator.distance.unit", 1500.0);

        // subscribe to Eventbus for incoming messages
        vertx.eventBus().consumer(config().getString(RES_INQUEUE, RES_INQUEUE), this::onMessage);

        defaultTime = config().getInteger("interval", 10000);

        long timerID = vertx.setPeriodic(defaultTime, id -> {

            List<Responder> toRemove = new ArrayList<>();
            List<Responder> toAdd = new ArrayList<>();

            vertx.<String>executeBlocking(fut->{
                responders.forEach(responder -> {
                    if(responder.isEmpty()) {
                        // remove responder from simulated list
                        toRemove.add(responder);

                        // Avoid Concurrent Modification
                        synchronized(this){
                            // Check if the same responder is waiting for another mission in queue
                            if (waitQueue.containsKey(responder.getResponderId())) {
                                Queue<Responder> q = waitQueue.get(responder.getResponderId());
                                if (!q.isEmpty())
                                    toAdd.add(q.poll());
                                else {
                                    // if queue was empty remove the responder from the map
                                    waitQueue.remove(responder.getResponderId());
                                }
                            }
                        }
                    }
                    else {
                        if(responder.isContinue()){
                            createMessage((responder));
                        }
                    }

                });
                responders.removeAll(toRemove);
                responders.addAll(toAdd);
               // logger.info(Json.encode("Added "+toAdd));
               // logger.info(Json.encode("Removed: "+toRemove));
               // logger.info(Json.encode("Wait Queue: "+waitQueue));

            }, res -> {
                if (res.succeeded()) {
                    logger.debug("executed");

                } else {
                    logger.fatal("error while excute blocking ");
                    res.cause().printStackTrace();
                    startFuture.fail(res.cause());
                }
            });
        });

    }

    protected void createMessage(Responder r) {
        r.calculateNextLocation();
        if (humanMessageCheck(r)) {
            if (r.peek().isWayPoint())
                r.setStatus(Responder.Status.PICKEDUP);

            else if (r.peek().isDestination())
                r.setStatus(Responder.Status.DROPPED);

            else
                r.setStatus(Responder.Status.MOVING);

            sendMessage(r);
            r.nextLocation();
        } else {
            sendMessage(r);
            r.nextLocation();
        }

    }

    // if human return false and skip this guy
    public boolean humanMessageCheck(Responder r){
        if(r.isHuman()){
            if (r.peek().isWayPoint()) {
                // eating one step
                r.setContinue(false);
                return false;
            }
            else if(r.peek().isDestination()){
                r.setContinue(false);
                r.setStatus(Responder.Status.DROPPED);
                return false;
            }
        }


        return true;

    }


    private void sendMessage(Responder r){

        com.redhat.cajun.navy.responder.simulator.Responder responder =
                new com.redhat.cajun.navy.responder.simulator.Responder(
                        r.getResponderId(),
                        r.getMissionId(),
                        r.getIncidentId(),
                        r.getLocation().getLat(),
                        r.getLocation().getLon(),
                        r.isHuman(),
                        r.isContinue(), r.getStatus().getActionType());

        DeliveryOptions options = new DeliveryOptions().addHeader("action", Action.PUBLISH_UPDATE.getActionType()).addHeader("key",r.getIncidentId()+r.getResponderId());
        vertx.eventBus().send(RES_OUTQUEUE, responder.toString(), options,
                reply -> {
                    if (!reply.succeeded()) {
                        logger.error("EventBus: Responder update message not accepted "+r);
                    }
                });

    }


    private void addResponder(MissionCommand mc){

        vertx.<String>executeBlocking(fut->{

            if (mc.getMessageType().equals(MessageType.MissionPickedUpEvent.getMessageType()) ||
                    mc.getMessageType().equals(MessageType.MissionCompletedEvent.getMessageType())) {
            }
            else {
                try {
                    logger.info(mc);
                        Responder r = getResponder(mc, MessageType.MissionStartedEvent);
                        r.setDistanceUnit(distanceUnit);
                        if (!responders.contains(r))
                            synchronized (this) {
                                responders.add(r);
                            }
                        else {
                            synchronized (this) {
                                if (waitQueue.containsKey(r.getResponderId())) {
                                    Queue<Responder> q = waitQueue.get(r.getResponderId());
                                    q.add(r);
                                    waitQueue.replace(r.getResponderId(), q);
                                } else {
                                    Queue<Responder> q = new LinkedList<>();
                                    q.add(r);
                                    waitQueue.put(r.getResponderId(), q);
                                }
                            }
                        }
                } catch (UnWantedResponderEvent re) {
                        re.printStackTrace();
                }
            }
        }, res -> {
            if (res.succeeded()) {
                logger.debug("executed");

            } else {
                logger.fatal("error while excute blocking ");
                res.cause().printStackTrace();

            }
        });

    }


    public void onMessage(Message<JsonObject> message) {

        if (!message.headers().contains("action")) {
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
            return;
        }
        String action = message.headers().get("action");
        switch (action) {
            case "CREATE_ENTRY":
                MissionCommand mc = Json.decodeValue(String.valueOf(message.body()), MissionCommand.class);
                addResponder(mc);
                message.reply("received");
                break;
            case "RESPONDER_MSG":
                Responder r = Json.decodeValue(String.valueOf(message.body()), Responder.class);
                synchronized (this){
                        setResponderStatus(r);
                }
                message.reply("request processed");
                break;

            default:
                message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
        }
    }

    protected void setResponderStatus(Responder r){
        if(responders.contains(r)) {
            for (Responder temp : responders) {
                if (temp.getResponderId().equals(r.getResponderId())) {
                    Responder.Status status = r.getStatus();
                    r = temp;
                    r.setHuman(true);
                    r.setStatus(status);
                    if (status.equals(Responder.Status.PICKEDUP) || status.equals(Responder.Status.DROPPED)) {
                        r.setContinue(true);
                        sendMessage(r);
                        r.nextLocation();
                    }
                    break;
                }
            }
            synchronized (this) {
                // remove previous version of responder in HashSet
                responders.remove(r);
                // add latest version of responder with setHuman=true
                responders.add(r);
            }
        }

    }


    protected Responder getResponder(MissionCommand mc, MessageType messageType) throws UnWantedResponderEvent{

        Mission m = mc.getBody();
        if(
                m.getResponderStartLat() == 0
                        || m.getResponderStartLong() == 0
                        || m.getIncidentLat() == 0
                        || m.getIncidentLong() == 0
                        || m.getDestinationLat() == 0
                        || m.getDestinationLong() == 0
        ){
            logger.fatal("Recieved 0 for coordinates, NOT ACCEPTED!");
            throw new UnWantedResponderEvent("Unwanted MessageType: "+messageType.getMessageType());
        }


        else if(MessageType.valueOf(mc.getMessageType()).equals(messageType)){
            Responder r = mc.getBody().getResponder();
                // need to change this to non-blocking
                getPersonResponder(r.getResponderId()).doOnError(throwable -> {
                    logger.error(throwable.getMessage());
                }).doOnSuccess(aBoolean -> {
                    r.setHuman(aBoolean);
                }).subscribe();

            return r;
        }

        else throw new UnWantedResponderEvent("Unwanted MessageType: "+messageType.getMessageType());
    }


    protected Single<Boolean> getPersonResponder(String responderId){
        return Single.fromFuture(getResponder(responderId));
    }

    protected CompletableFuture<Boolean> getResponder(String id){

        CompletableFuture<Boolean> request = new CompletableFuture<>();
        client.get(port, host, uri+id)
                .send(ar -> {
                    if (ar.succeeded()) {
                        // Obtain response
                        HttpResponse<Buffer> response = ar.result();
                        JsonObject obj = response.bodyAsJsonObject();
                        logger.debug("Received response with status code" + response.statusCode());
                        logger.info("ResponderService: " + obj);
                        request.complete(obj.getBoolean("person"));
                    } else {
                        logger.error("Responder not found: defaulting to false" + ar.cause().getMessage());
                        request.complete(false);
                    }
                });

            request.complete(false);
            return request;


    }


}

