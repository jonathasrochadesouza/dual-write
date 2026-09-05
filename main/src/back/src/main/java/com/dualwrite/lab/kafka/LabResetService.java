package com.dualwrite.lab.kafka;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dualwrite.lab.experiment.ExperimentRepository;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.outbox.OutboxRepository;

@Service
public class LabResetService {

    private final ExperimentRepository experimentRepository;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final String bootstrapServers;
    private final String topic;

    public LabResetService(
            ExperimentRepository experimentRepository,
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${dualwrite.kafka.topic}") String topic
    ) {
        this.experimentRepository = experimentRepository;
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.bootstrapServers = bootstrapServers;
        this.topic = topic;
    }

    public ResetResult reset() {
        experimentRepository.deleteAll();
        orderRepository.deleteAll();
        outboxRepository.deleteAll();

        boolean kafkaReset = false;
        try {
            resetKafkaTopic();
            kafkaReset = true;
        } catch (Exception ignored) {
        }

        return new ResetResult(kafkaReset);
    }

    private void resetKafkaTopic() throws ExecutionException, InterruptedException {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient admin = AdminClient.create(props)) {
            admin.deleteTopics(Collections.singleton(topic)).all().get();
            NewTopic newTopic = new NewTopic(topic, (short) 1, (short) 1);
            admin.createTopics(Collections.singleton(newTopic)).all().get();
        }
    }

    public record ResetResult(boolean kafkaReset) {}
}
