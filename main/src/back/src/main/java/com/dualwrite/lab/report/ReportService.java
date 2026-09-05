package com.dualwrite.lab.report;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dualwrite.lab.experiment.Experiment;
import com.dualwrite.lab.experiment.ExperimentRepository;
import com.dualwrite.lab.kafka.KafkaMessageInspector;
import com.dualwrite.lab.order.OrderRepository;
import com.dualwrite.lab.outbox.OutboxRepository;

@Service
public class ReportService {

    private final ExperimentRepository experimentRepository;
    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final KafkaMessageInspector kafkaMessageInspector;
    private final VerdictDeriver verdictDeriver;

    public ReportService(
            ExperimentRepository experimentRepository,
            OrderRepository orderRepository,
            OutboxRepository outboxRepository,
            KafkaMessageInspector kafkaMessageInspector,
            VerdictDeriver verdictDeriver
    ) {
        this.experimentRepository = experimentRepository;
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.kafkaMessageInspector = kafkaMessageInspector;
        this.verdictDeriver = verdictDeriver;
    }

    public ExperimentReport buildReport(UUID experimentId) {
        Experiment experiment = experimentRepository.findById(experimentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experiment not found"));

        ObservedState observed = inspect(experimentId);
        ExpectedOutcome expected = ExpectedOutcome.forScenario(experiment.scenario());
        Verdict verdict = experiment.verdict() != null
                ? experiment.verdict()
                : verdictDeriver.derive(experiment.scenario(), observed);

        return new ExperimentReport(
                experiment.id(),
                experiment.scenario(),
                experiment.executedAt(),
                expected,
                observed,
                verdict,
                expected.description()
        );
    }

    public ObservedState inspect(UUID experimentId) {
        return new ObservedState(
                orderRepository.findByExperimentId(experimentId),
                outboxRepository.findByExperimentId(experimentId),
                kafkaMessageInspector.findPayloadsByExperimentId(experimentId)
        );
    }

    public List<Experiment> history() {
        return experimentRepository.findAllOrderByExecutedAtDesc();
    }
}
