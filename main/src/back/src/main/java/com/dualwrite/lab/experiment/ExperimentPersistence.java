package com.dualwrite.lab.experiment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExperimentPersistence {

    private final ExperimentRepository experimentRepository;

    public ExperimentPersistence(ExperimentRepository experimentRepository) {
        this.experimentRepository = experimentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Experiment save(Experiment experiment) {
        return experimentRepository.save(experiment);
    }
}
