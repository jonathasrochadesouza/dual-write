package com.dualwrite.lab.outbox;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOutboxRepository implements OutboxRepository {

    private static final RowMapper<OutboxEvent> ROW_MAPPER = (rs, rowNum) -> new OutboxEvent(
            rs.getObject("id", UUID.class),
            rs.getObject("experiment_id", UUID.class),
            rs.getObject("aggregate_id", UUID.class),
            rs.getString("event_type"),
            rs.getString("payload"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcOutboxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OutboxEvent save(OutboxEvent event) {
        jdbcTemplate.update(
                """
                INSERT INTO outbox_events (id, experiment_id, aggregate_id, event_type, payload, status, created_at)
                VALUES (?, ?, ?, ?, CAST(? AS jsonb), ?, ?)
                """,
                event.id(),
                event.experimentId(),
                event.aggregateId(),
                event.eventType(),
                event.payload(),
                event.status(),
                Timestamp.from(event.createdAt())
        );
        return event;
    }

    @Override
    public List<OutboxEvent> findByExperimentId(UUID experimentId) {
        return jdbcTemplate.query(
                """
                SELECT id, experiment_id, aggregate_id, event_type, payload::text AS payload, status, created_at
                FROM outbox_events
                WHERE experiment_id = ?
                """,
                ROW_MAPPER,
                experimentId
        );
    }

    @Override
    public long countByExperimentId(UUID experimentId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox_events WHERE experiment_id = ?",
                Long.class,
                experimentId
        );
        return count == null ? 0L : count;
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM outbox_events");
    }
}
