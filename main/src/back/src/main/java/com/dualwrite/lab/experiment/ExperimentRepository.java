package com.dualwrite.lab.experiment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperimentRepository {
    Experiment save(Experiment experiment);

    Optional<Experiment> findById(UUID id);

    List<Experiment> findAllOrderByExecutedAtDesc();
}
