package com.dualwrite.lab.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessageInspector {

    private final String bootstrapServers;
    private final String topic;

    public KafkaMessageInspector(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${dualwrite.kafka.topic}") String topic
    ) {
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

    public List<String> findPayloadsByExperimentId(UUID experimentId) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dual-write-lab-inspector-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        List<String> payloads = new ArrayList<>();
        String experimentToken = experimentId.toString();

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(topic));
            long deadline = System.currentTimeMillis() + 3_000L;
            int emptyPolls = 0;
            while (System.currentTimeMillis() < deadline && emptyPolls < 5) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(300));
                if (records.isEmpty()) {
                    emptyPolls++;
                    continue;
                }
                emptyPolls = 0;
                for (ConsumerRecord<String, String> record : records) {
                    String value = record.value();
                    if (value != null && value.contains(experimentToken)) {
                        payloads.add(value);
                    }
                }
            }
        }
        return payloads;
    }
}
