package com.redhat.cajun.navy.responder.simulator;

import static com.redhat.cajun.navy.responder.simulator.EventConfig.RES_INQUEUE;

import java.util.HashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;

public class ResponderConsumerVerticle extends AbstractVerticle {

    Logger logger = LoggerFactory.getLogger(ResponderConsumerVerticle.class);
    private Map<String, String> config = new HashMap<>();
    KafkaConsumer<String, String> consumer = null;
    public String responderUpdatedTopic = null;


    @Override
    public void start(Future<Void> startFuture) throws Exception {

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config().getString("kafka.connect", "localhost:9092"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, config().getString("kafka.group.id"));
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,config().getBoolean("kafka.autocommit", true).toString());
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, config().getString("kafka.security.protocol"));
        config.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, config().getString("kafka.ssl.keystore.type"));
        config.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, config().getString("kafka.ssl.keystore.location"));
        config.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, config().getString("kafka.ssl.keystore.password"));
        config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, config().getString("kafka.ssl.truststore.type"));
        config.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, config().getString("kafka.ssl.truststore.location"));
        config.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, config().getString("kafka.ssl.truststore.password"));

        responderUpdatedTopic = config().getString("kafka.sub");

        consumer = KafkaConsumer.create(vertx, config);

        consumer.handler(record -> {
            DeliveryOptions options = new DeliveryOptions().addHeader("action", Action.CREATE_ENTRY.getActionType());

            vertx.eventBus().send(RES_INQUEUE, record.value(), options, reply -> {
                if (reply.succeeded()) {
                    logger.debug("Incoming message accepted");
                    logger.debug("Message committed "+record.value());
                } else {
                    logger.error("Incoming Message not accepted "+record.value());


                }
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