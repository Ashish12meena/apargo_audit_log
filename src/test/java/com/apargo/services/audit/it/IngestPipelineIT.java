package com.apargo.services.audit.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import com.apargo.platform.contract.audit.AuditEventDto;
import com.apargo.services.audit.TestEvents;
import com.apargo.services.audit.common.constant.DeadLetterHeaders;
import com.apargo.services.audit.common.constant.MongoCollections;
import com.apargo.services.audit.infrastructure.config.MongoConfig;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 * End-to-end: Kafka → batch listener → MongoDB, with duplicates and a poison record.
 * Run with {@code mvn verify -Pit} (needs Docker).
 */
@SpringBootTest
@Testcontainers
class IngestPipelineIT {

    private static final String AUDIT_TOPIC = "platform.audit.events";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer("apache/kafka:3.8.0");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.kafka.consumer.fetch-max-wait", () -> "100ms");
        registry.add("audit.mongo.write-uri", MONGO::getReplicaSetUrl);
        registry.add("audit.mongo.database", () -> "audit_it");
        registry.add("audit.environment", () -> TestEvents.ENVIRONMENT);
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    @Qualifier(MongoConfig.WRITE_DATABASE)
    private MongoDatabase database;

    @Autowired
    private KafkaTemplate<String, byte[]> kafkaTemplate;

    @Test
    void storesEachEventOnceAndDeadLettersPoison() throws Exception {
        AuditEventDto event = TestEvents.auditBuilder().occurredAt(Instant.now().minusSeconds(5)).build();
        byte[] json = TestEvents.json(event);

        kafkaTemplate.send(AUDIT_TOPIC, event.eventId(), json).get();
        kafkaTemplate.send(AUDIT_TOPIC, event.eventId(), json).get();
        kafkaTemplate.send(AUDIT_TOPIC, "poison", "{not json".getBytes(StandardCharsets.UTF_8)).get();

        var auditLogs = database.getCollection(MongoCollections.AUDIT_LOGS);
        awaitTrue(() -> auditLogs.countDocuments(Filters.eq("_id", event.eventId())) == 1);

        Document stored = auditLogs.find(Filters.eq("_id", event.eventId())).first();
        assertThat(stored.getString("eventType")).isEqualTo("TEMPLATE_UPDATED");
        assertThat(stored.get("orgId")).isEqualTo(1001L);
        assertThat(stored.getList("changes", Document.class).get(0).getString("oldValue")).isEqualTo("DRAFT");
        assertThat(stored.getDate("archiveAt")).isAfter(stored.getDate("recordedAt"));

        ConsumerRecord<String, byte[]> deadLetter = readOne(AUDIT_TOPIC + ".dlq");
        assertThat(deadLetter.key()).isEqualTo("poison");
        assertThat(new String(deadLetter.value(), StandardCharsets.UTF_8)).isEqualTo("{not json");
        assertThat(headerText(deadLetter, DeadLetterHeaders.REASON)).isEqualTo("DESERIALIZATION");

        assertThat(auditLogs.countDocuments()).isEqualTo(1);
    }

    private static ConsumerRecord<String, byte[]> readOne(String topic) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "it-dlq-reader",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class);
        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(config)) {
            consumer.subscribe(List.of(topic));
            Instant deadline = Instant.now().plus(TIMEOUT);
            while (Instant.now().isBefore(deadline)) {
                for (ConsumerRecord<String, byte[]> record : consumer.poll(Duration.ofMillis(500))) {
                    return record;
                }
            }
        }
        throw new AssertionError("No record on " + topic + " within " + TIMEOUT);
    }

    private static String headerText(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        Instant deadline = Instant.now().plus(TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Condition not met within " + TIMEOUT);
    }
}
