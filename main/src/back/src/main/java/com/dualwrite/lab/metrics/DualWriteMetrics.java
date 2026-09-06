package com.dualwrite.lab.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import com.dualwrite.lab.report.Verdict;
import com.dualwrite.lab.scenario.shared.ScenarioId;

@Component
public class DualWriteMetrics {

    private final MeterRegistry meterRegistry;

    public DualWriteMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ---- DB ----

    public void recordDbWrite(ScenarioId scenario) {
        counter("dualwrite.orders.total", scenario).increment();
    }

    public void recordDatabaseWriteFailure(ScenarioId scenario) {
        counter("dualwrite.failures.db.total", scenario).increment();
    }

    // ---- Kafka ----

    public void recordKafkaPublish(ScenarioId scenario) {
        counter("dualwrite.events.published.total", scenario).increment();
    }

    public void recordKafkaPublishFailure(ScenarioId scenario) {
        counter("dualwrite.failures.kafka.total", scenario).increment();
    }

    // ---- Outbox ----

    public void recordOutboxCommitted(ScenarioId scenario) {
        counter("dualwrite.outbox.rows.total", scenario).increment();
    }

    // ---- Experiments ----

    public void recordExperiment(ScenarioId scenario, Verdict verdict) {
        Counter.builder("dualwrite.experiments.total")
                .tag("scenario", scenario.name())
                .tag("verdict", verdict.name())
                .register(meterRegistry)
                .increment();
    }

    // ---- Timers ----

    public Timer.Sample startScenarioTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopScenarioTimer(Timer.Sample sample, ScenarioId scenario) {
        sample.stop(Timer.builder("dualwrite.scenario.duration")
                .tag("scenario", scenario.name())
                .register(meterRegistry));
    }

    public Timer.Sample startKafkaTimer() {
        return Timer.start(meterRegistry);
    }

    public void stopKafkaTimer(Timer.Sample sample, ScenarioId scenario) {
        sample.stop(Timer.builder("dualwrite.kafka.publish.latency")
                .tag("scenario", scenario.name())
                .register(meterRegistry));
    }

    private Counter counter(String name, ScenarioId scenario) {
        return Counter.builder(name)
                .tag("scenario", scenario.name())
                .register(meterRegistry);
    }
}
