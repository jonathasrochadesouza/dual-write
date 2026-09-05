package com.dualwrite.lab.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dualwrite.lab.experiment.Experiment;
import com.dualwrite.lab.experiment.ExperimentRunner;
import com.dualwrite.lab.report.ExperimentReport;
import com.dualwrite.lab.report.ReportService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/experiments")
@Validated
public class ExperimentController {

    private final ExperimentRunner experimentRunner;
    private final ReportService reportService;

    public ExperimentController(ExperimentRunner experimentRunner, ReportService reportService) {
        this.experimentRunner = experimentRunner;
        this.reportService = reportService;
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
}
