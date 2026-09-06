package com.dualwrite.lab.experiment;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.dualwrite.lab.report.Verdict;
import com.dualwrite.lab.scenario.shared.ScenarioId;

@Repository
public class JdbcExperimentRepository implements ExperimentRepository {

    private static final RowMapper<Experiment> ROW_MAPPER = (rs, rowNum) -> {
        String verdictValue = rs.getString("verdict");
        return new Experiment(
                rs.getObject("id", UUID.class),
                ScenarioId.valueOf(rs.getString("scenario")),
                rs.getTimestamp("executed_at").toInstant(),
                verdictValue == null ? null : Verdict.valueOf(verdictValue)
        );
    };

    private final JdbcTemplate jdbcTemplate;

    public JdbcExperimentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Experiment save(Experiment experiment) {
        jdbcTemplate.update(
                """
                INSERT INTO experiments (id, scenario, executed_at, verdict)
                VALUES (?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE SET verdict = EXCLUDED.verdict
                """,
                experiment.id(),
                experiment.scenario().name(),
                Timestamp.from(experiment.executedAt()),
                experiment.verdict() == null ? null : experiment.verdict().name()
        );
        return experiment;
    }

    @Override
    public Optional<Experiment> findById(UUID id) {
        List<Experiment> experiments = jdbcTemplate.query(
                "SELECT id, scenario, executed_at, verdict FROM experiments WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return experiments.stream().findFirst();
    }

    @Override
    public List<Experiment> findAllOrderByExecutedAtDesc() {
        return jdbcTemplate.query(
                "SELECT id, scenario, executed_at, verdict FROM experiments ORDER BY executed_at DESC",
                ROW_MAPPER
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM experiments");
    }
}
