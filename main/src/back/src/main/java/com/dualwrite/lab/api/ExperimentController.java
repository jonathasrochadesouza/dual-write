package com.dualwrite.lab.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dualwrite.lab.experiment.Experiment;
import com.dualwrite.lab.experiment.ExperimentRunner;
import com.dualwrite.lab.kafka.LabResetService;
import com.dualwrite.lab.report.ExperimentReport;
import com.dualwrite.lab.report.ReportService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/experiments")
@Validated
public class ExperimentController {

    private final ExperimentRunner experimentRunner;
    private final ReportService reportService;
    private final LabResetService labResetService;

    public ExperimentController(
            ExperimentRunner experimentRunner,
            ReportService reportService,
            LabResetService labResetService
    ) {
        this.experimentRunner = experimentRunner;
        this.reportService = reportService;
        this.labResetService = labResetService;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.OK)
    public ExperimentReport run(@Valid @RequestBody RunExperimentRequest request) {
        return experimentRunner.run(request.scenario(), request.customerId(), request.total());
    }

    @GetMapping("/{id}/report")
    public ExperimentReport report(@PathVariable UUID id) {
        return reportService.buildReport(id);
    }

    @GetMapping
    public List<Experiment> history() {
        return reportService.history();
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public LabResetService.ResetResult reset() {
        return labResetService.reset();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
