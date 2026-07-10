/*
 * SPDX-License-Identifier: Apache-2.0
 * © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
 * attributed to the Department for Business and Trade (UK) as the governing entity.
 */

package uk.gov.dbt.ndtp.federator.certificate.manager.config.logging;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Emits a periodic {@code component.heartbeat} log event so the health-monitoring dashboard's
 * Component Health State / Pipeline Health State panels register this service as alive. The event
 * shape matches the federator's HeartbeatService: an "event.name" key-value pair identifies the
 * heartbeat, "component.name" groups it on the dashboard, "component.status" drives the health
 * colour, and the numeric heartbeat.* / component.uptime_seconds values are carried as SLF4J 2.x
 * key/value pairs so they reach the OTLP log record as typed attributes (via the OTEL appender's
 * captureKeyValuePairAttributes in logback-spring.xml) and, from there, the otel-logs topic.
 *
 * <p>Interval is configurable via {@code heartbeat.interval-seconds} (default 900s / 15 min, as in
 * the federator). For local verification set it low, e.g. HEARTBEAT_INTERVAL_SECONDS=30.
 */
@Component
public class OtelHeartbeatEmitter {

    private static final DateTimeFormatter HEARTBEAT_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx");

    private final Logger logger;
    private final String componentName;
    private final long intervalSeconds;
    private final Instant startTime = Instant.now();
    private final AtomicLong sequence = new AtomicLong(0);

    public OtelHeartbeatEmitter(
            @Value("${management.observations.key-values.service.name:certificate-manager}") String componentName,
            @Value("${otel.heartbeat.interval-seconds:900}") long intervalSeconds) {
        // logger.name == component/service name, matching the federator convention.
        this.logger = LoggerFactory.getLogger(componentName);
        this.componentName = componentName;
        this.intervalSeconds = intervalSeconds;
    }

    /**
     * Fires immediately on startup, then every {@code heartbeat.interval-seconds}. Never throws:
     * a failed beat must not stop the scheduler.
     */
    @Scheduled(
            initialDelayString = "0",
            fixedDelayString = "${heartbeat.interval-seconds:900}000")
    public void emit() {
        try {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            long uptime = Duration.between(startTime, Instant.now()).getSeconds();
            long seq = sequence.incrementAndGet();

            // event.name MUST be present for the dashboard's component.heartbeat filter. The other
            // key/value pairs are emitted as typed attributes (numbers stay numbers).
            logger.atInfo()
                    .addKeyValue("event.name", "component.heartbeat")
                    .addKeyValue("component.name", componentName)
                    .addKeyValue("component.status", "healthy")
                    .addKeyValue("heartbeat.timestamp", now.format(HEARTBEAT_TS))
                    .addKeyValue("heartbeat.sequence", seq)
                    .addKeyValue("component.uptime_seconds", uptime)
                    .addKeyValue("heartbeat.interval_seconds", intervalSeconds)
                    .log("Heartbeat: {} is {}", componentName, "healthy");
        } catch (Exception e) {
            logger.atWarn()
                    .addKeyValue("component.name", componentName)
                    .addKeyValue("error.type", e.getClass().getSimpleName())
                    .addKeyValue("error.message", e.getMessage())
                    .log("Heartbeat emit failed for {}", componentName);
        }
    }
}