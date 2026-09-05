package com.dualwrite.lab.order;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrderRepository implements OrderRepository {

    private static final RowMapper<Order> ROW_MAPPER = (rs, rowNum) -> new Order(
            rs.getObject("id", UUID.class),
            rs.getObject("experiment_id", UUID.class),
            rs.getString("customer_id"),
            rs.getBigDecimal("total"),
            rs.getString("status"),
            rs.getTimestamp("created_at").toInstant()
    );

    private final JdbcTemplate jdbcTemplate;

    public JdbcOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Order save(Order order) {
        jdbcTemplate.update(
                """
                INSERT INTO orders (id, experiment_id, customer_id, total, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                order.id(),
                order.experimentId(),
                order.customerId(),
                order.total(),
                order.status(),
                Timestamp.from(order.createdAt())
        );
        return order;
    }

    @Override
    public Optional<Order> findById(UUID id) {
        List<Order> orders = jdbcTemplate.query(
                "SELECT id, experiment_id, customer_id, total, status, created_at FROM orders WHERE id = ?",
                ROW_MAPPER,
                id
        );
        return orders.stream().findFirst();
    }

    @Override
    public List<Order> findByExperimentId(UUID experimentId) {
        return jdbcTemplate.query(
                "SELECT id, experiment_id, customer_id, total, status, created_at FROM orders WHERE experiment_id = ?",
                ROW_MAPPER,
                experimentId
        );
    }

    @Override
    public long countByExperimentId(UUID experimentId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE experiment_id = ?",
                Long.class,
                experimentId
        );
        return count == null ? 0L : count;
    }
}
