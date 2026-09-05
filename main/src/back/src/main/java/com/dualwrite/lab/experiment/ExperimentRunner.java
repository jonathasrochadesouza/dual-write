package com.dualwrite.lab.experiment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dualwrite.lab.fault.SimulatedOutageException;
import com.dualwrite.lab.metrics.DualWriteMetrics;
import com.dualwrite.lab.report.ExperimentReport;
import com.dualwrite.lab.report.ObservedState;
import com.dualwrite.lab.report.ReportService;
import com.dualwrite.lab.report.Verdict;
import com.dualwrite.lab.report.VerdictDeriver;
import com.dualwrite.lab.scenario.ScenarioContext;
import com.dualwrite.lab.scenario.ScenarioId;
import com.dualwrite.lab.scenario.ScenarioPort;
import com.dualwrite.lab.scenario.ScenarioRegistry;

@Service
public class ExperimentRunner {

    private final ScenarioRegistry scenarioRegistry;
    private final ExperimentPersistence experimentPersistence;
    private final ReportService reportService;
    private final VerdictDeriver verdictDeriver;
    private final DualWriteMetrics metrics;

    public ExperimentRunner(
            ScenarioRegistry scenarioRegistry,
            ExperimentPersistence experimentPersistence,
            ReportService reportService,
            VerdictDeriver verdictDeriver,
            DualWriteMetrics metrics
    ) {
        this.scenarioRegistry = scenarioRegistry;
        this.experimentPersistence = experimentPersistence;
        this.reportService = reportService;
        this.verdictDeriver = verdictDeriver;
        this.metrics = metrics;
    }

    public ExperimentReport run(ScenarioId scenarioId, String customerId, BigDecimal total) {
        UUID experimentId = UUID.randomUUID();
        Instant executedAt = Instant.now();
        experimentPersistence.save(new Experiment(experimentId, scenarioId, executedAt, null));

        ScenarioPort scenario = scenarioRegistry.require(scenarioId);
        ScenarioContext context = new ScenarioContext(
                experimentId,
                scenarioId,
                customerId == null || customerId.isBlank() ? "customer-42" : customerId,
                total == null ? new BigDecimal("199.90") : total
        );

        try {
            scenario.execute(context);
        } catch (SimulatedOutageException ignored) {
            // Expected for fault-injected scenarios.
        }

        sleepBriefly();

        ObservedState observed = reportService.inspect(experimentId);
        Verdict verdict = verdictDeriver.derive(scenarioId, observed);
        experimentPersistence.save(new Experiment(experimentId, scenarioId, executedAt, verdict));
        metrics.recordExperiment(scenarioId, verdict);

        return reportService.buildReport(experimentId);
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(500L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
