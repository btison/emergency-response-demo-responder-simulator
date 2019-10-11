package com.redhat.cajun.navy.responder.simulator;

import static com.redhat.cajun.navy.responder.simulator.EventConfig.RES_INQUEUE;

import java.util.HashMap;
import java.util.Map;

import com.redhat.cajun.navy.responder.simulator.tracing.TracingKafkaConsumer;
import com.redhat.cajun.navy.responder.simulator.tracing.TracingUtils;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.consumer.KafkaConsumer;

public class ResponderConsumerVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(ResponderConsumerVerticle.class);
    private Map<String, String> config = new HashMap<>();
    private KafkaConsumer<String, String> consumer = null;

    private Tracer tracer;

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        tracer = GlobalTracer.get();

        config.put("bootstrap.servers", config().getString("kafka.connect", "localhost:9092"));
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", config().getString("kafka.group.id"));
        config.put("auto.offset.reset", "earliest");
        config.put("enable.auto.commit",config().getBoolean("kafka.autocommit", true).toString());

        String responderUpdatedTopic = config().getString("kafka.sub");

        consumer = TracingKafkaConsumer.create(vertx, config, tracer);

        consumer.handler(record -> {

            //we're only interested in MissionStartedEvent
            JsonObject msg = new  JsonObject(record.value());
            String messageType = msg.getString("messageType");
            if (!SimulationControl.MessageType.MissionStartedEvent.getMessageType().equals(messageType)) {
                return;
            }

            Span span = TracingUtils.buildChildSpan("missionStartedEvent", record, tracer);

            DeliveryOptions options = new DeliveryOptions().addHeader("action", Action.CREATE_ENTRY.getActionType());
            TracingUtils.injectInEventBusMessage(span.context(), options, tracer);
            vertx.eventBus().send(RES_INQUEUE, record.value(), options, reply -> {
                if (reply.succeeded()) {
                    logger.debug("Incoming message accepted");
                    logger.debug("Message committed " + record.value());
                } else {
                    logger.error("Incoming Message not accepted " + record.value());
                }
                span.finish();
            });
        });

        consumer.subscribe(responderUpdatedTopic, ar -> {
            if (ar.succeeded()) {
                logger.info(("subscribed to MissionEvents"));
            } else {
                logger.fatal("Could not subscribe " + ar.cause().getMessage());
            }
        });
    }


    @Override
    public void stop() throws Exception {
        consumer.unsubscribe(ar -> {

            if (ar.succeeded()) {
                logger.debug("Consumer unsubscribed");
            }
        });
    }
}