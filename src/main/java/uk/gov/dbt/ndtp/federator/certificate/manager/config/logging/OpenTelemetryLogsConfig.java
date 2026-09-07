package uk.gov.dbt.ndtp.federator.certificate.manager.config.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Connects the Logback {@code OpenTelemetryAppender} (declared in logback-spring.xml) to the
 * {@link OpenTelemetry} instance Spring Boot 4.1's native autoconfiguration builds from
 * management.opentelemetry.* / management.opentelemetry.logging.export.otlp.* properties
 * (endpoint, TLS via spring.ssl.bundle, resource attributes, etc. - see application.yml).
 *
 * <p>Spring Boot wires up the OTLP log exporter itself but does not bridge Logback to it
 * automatically, so this install() call is still required; until it runs the appender has no
 * OpenTelemetry instance and drops logs, which is why traces could reach the Collector but logs
 * never did before this bean existed. Logs emitted before this point are buffered by the appender
 * and replayed on install. Runs in afterPropertiesSet() (bean initialization) rather than an
 * ApplicationReadyEvent, so it happens as early as possible relative to the rest of startup.
 */
@Component
public class OpenTelemetryLogsConfig implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    public OpenTelemetryLogsConfig(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
