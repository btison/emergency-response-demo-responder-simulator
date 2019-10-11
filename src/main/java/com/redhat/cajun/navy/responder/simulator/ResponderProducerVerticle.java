package com.redhat.cajun.navy.responder.simulator;

import static com.redhat.cajun.navy.responder.simulator.EventConfig.RES_OUTQUEUE;

import java.util.HashMap;
import java.util.Map;

import com.redhat.cajun.navy.responder.simulator.tracing.TracingKafkaProducer;
import com.redhat.cajun.navy.responder.simulator.tracing.TracingUtils;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;

public class ResponderProducerVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(ResponderProducerVerticle.class);
    private Map<String, String> config = new HashMap<>();
    private KafkaProducer<String,String> producer = null;
    private String responderMovedTopic = null;

    private Tracer tracer;


    @Override
    public void start(Future<Void> startFuture) throws Exception {

        tracer = GlobalTracer.get();

        config.put("bootstrap.servers", config().getString("kafka.connect", "localhost:9092"));
        config.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        config.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        responderMovedTopic = config().getString("kafka.pub");

        producer = TracingKafkaProducer.create(vertx, config, tracer);
        vertx.eventBus().consumer(config().getString(RES_OUTQUEUE, RES_OUTQUEUE), this::onMessage);
    }


    public void onMessage(Message<JsonObject> message) {

        if (!message.headers().contains("action")) {
            message.fail(ErrorCodes.NO_ACTION_SPECIFIED.ordinal(), "No action header specified");
            return;
        }


        String action = message.headers().get("action");
        String key = message.headers().get("key");
        Span span = TracingUtils.buildChildSpan(action, message, tracer);
        try (Scope scope = tracer.activateSpan(span)) {
            switch (action) {
                case "PUBLISH_UPDATE":

                    KafkaProducerRecord<String, String> record =
                            KafkaProducerRecord.create(responderMovedTopic, key, String.valueOf(message.body()));
                    System.out.println(message.body());
                    producer.write(record, done -> {

                        if (done.succeeded()) {

                            RecordMetadata recordMetadata = done.result();
                        /*logger.info("Message " + record.value() + " written on topic=" + recordMetadata.getTopic() +
                                ", partition=" + recordMetadata.getPartition() +
                                ", offset=" + recordMetadata.getOffset());
*/
                            message.reply("Message delivered to topic" + responderMovedTopic);
                        }

                    });

                    break;

                default:
                    message.fail(ErrorCodes.BAD_ACTION.ordinal(), "Bad action: " + action);
            }
        } finally {
            span.finish();
        }
    }


}
