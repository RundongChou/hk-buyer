package com.hkbuyer.repository;

import com.hkbuyer.domain.TimelineEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class TimelineRepository {

    private final JdbcTemplate jdbcTemplate;

    public TimelineRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addEvent(Long orderId, String eventType, String eventDescription) {
        String sql = "INSERT INTO order_timeline_event(order_id, event_type, event_description, created_at) VALUES(?, ?, ?, NOW())";
        jdbcTemplate.update(sql, orderId, eventType, eventDescription);
    }

    public List<TimelineEvent> listByOrderId(Long orderId) {
        String sql = "SELECT event_id, order_id, event_type, event_description, created_at " +
                "FROM order_timeline_event WHERE order_id = ? ORDER BY created_at ASC, event_id ASC";
        return jdbcTemplate.query(sql, timelineRowMapper(), orderId);
    }

    private RowMapper<TimelineEvent> timelineRowMapper() {
        return (rs, rowNum) -> {
            TimelineEvent event = new TimelineEvent();
            event.setEventId(rs.getLong("event_id"));
            event.setOrderId(rs.getLong("order_id"));
            event.setEventType(rs.getString("event_type"));
            event.setEventDescription(rs.getString("event_description"));
            Timestamp createdAt = rs.getTimestamp("created_at");
            event.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
            return event;
        };
    }
}
