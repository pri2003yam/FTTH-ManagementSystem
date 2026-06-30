package com.aaha.ftth.reservation.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationCleanup {

    private static final Logger logger = LoggerFactory.getLogger(ReservationCleanup.class);

    private final JdbcTemplate jdbcTemplate;

    public ReservationCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedDelay = 30000)
    public void expireStaleReservations() {
        int expired = jdbcTemplate.update(
                "UPDATE port_reservations SET status = 'EXPIRED' WHERE status = 'RESERVED' AND expires_at < NOW()");
        if (expired > 0) {
            logger.info("Expired {} stale reservation(s)", expired);
        }
    }
}
