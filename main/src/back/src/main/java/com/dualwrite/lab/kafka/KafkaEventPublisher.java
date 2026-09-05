package com.dualwrite.lab.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.dualwrite.lab.scenario.ScenarioId;
import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topic;
    private final DualWriteMetrics metrics;

    public KafkaEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${dualwrite.kafka.topic}") String topic,
            DualWriteMetrics metrics
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topic = topic;
        this.metrics = metrics;
    }

    @Override
    public void publish(OrderCreatedEvent event) {
        publish(event, null);
    }

    public void publish(OrderCreatedEvent event, ScenarioId scenario) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    event.orderId().toString(),
                    payload
            );
            record.headers().add(new RecordHeader("experimentId", event.experimentId().toString().getBytes()));
            if (scenario != null) {
                var sample = metrics.startKafkaTimer();
                kafkaTemplate.send(record).get();
                metrics.stopKafkaTimer(sample, scenario);
            } else {
                kafkaTemplate.send(record).get();
            }
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize order event", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to publish order event", ex);
        }
    }
}
