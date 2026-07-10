package uk.gov.dbt.ndtp.federator.certificate.manager.config.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Logback encoder that emits single-line JSON log records matching the OpenTelemetry Log Data
 * Model, per OTEL_LOG_SAMPLES.md: timestamp/observed_timestamp, severity_number/severity_text,
 * body, resource.{service.name,service.version}, attributes.{code.filepath,code.lineno,
 * code.function,logger.name}, and the conditional top-level trace_id/span_id/trace_flags.
 *
 * <p>Unlike a hand-rolled OTel SDK integration, this application uses Spring Boot's Micrometer
 * Tracing bridge (see pom.xml: micrometer-tracing-bridge-otel, opentelemetry-exporter-otlp;
 * application.yml: management.tracing.*). Micrometer Tracing populates the active span's
 * trace/span IDs into SLF4J's MDC automatically under the keys "traceId" and "spanId" - this
 * encoder reads those MDC values directly, rather than calling
 * io.opentelemetry.api.trace.Span.current() (which requires a manually-managed OTel SDK
 * instance this project deliberately does not create - Micrometer manages that internally).
 *
 * <p>trace_flags is not exposed via Micrometer's MDC integration, so when a trace is active this
 * encoder emits the W3C default sampled flag ("01"), matching what Micrometer's OTel bridge
 * itself sends on the wire for any span it has decided to export (consistent with
 * management.tracing.sampling.probability, which controls export, not the flag value emitted
 * here for in-process correlation).
 */
public class OtelJsonEncoder extends EncoderBase<ILoggingEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ISO_INSTANT;

    /** MDC key Micrometer Tracing's Logback integration populates with the active trace ID. */
    private static final String MDC_TRACE_ID = "traceId";

    /** MDC key Micrometer Tracing's Logback integration populates with the active span ID. */
    private static final String MDC_SPAN_ID = "spanId";

    /** Default W3C trace flags value (sampled) emitted when a trace is active. */
    private static final String DEFAULT_TRACE_FLAGS = "01";

    private String serviceName = "certificate-manager";
    private String serviceVersion;

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        Map<String, Object> log = new LinkedHashMap<>();

        String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(event.getTimeStamp()));
        log.put("timestamp", timestamp);
        log.put("observed_timestamp", timestamp);

        log.put("severity_number", mapSeverityNumber(event.getLevel()));
        log.put("severity_text", event.getLevel().toString());

        log.put("body", buildBody(event));

        Map<String, Object> resource = new LinkedHashMap<>();
        resource.put("service.name", resolveServiceName());
        resource.put("service.version", resolveServiceVersion());
        log.put("resource", resource);

        log.put("attributes", buildAttributes(event));

        // Conditional, top-level: present only when Micrometer Tracing has an active span on
        // this thread (populated into MDC under "traceId"/"spanId"). Both or neither - matches
        // OTEL_LOG_TRACE_CONTEXT_EXPLAINED.md's "active span" rule.
        Map<String, String> mdc = event.getMDCPropertyMap();
        String traceId = mdc != null ? mdc.get(MDC_TRACE_ID) : null;
        String spanId = mdc != null ? mdc.get(MDC_SPAN_ID) : null;
        if (traceId != null && !traceId.isBlank() && spanId != null && !spanId.isBlank()) {
            log.put("trace_id", traceId);
            log.put("span_id", spanId);
            log.put("trace_flags", DEFAULT_TRACE_FLAGS);
        }

        try {
            return (MAPPER.writeValueAsString(log) + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            String fallback = "{\"severity_text\":\"ERROR\",\"body\":\"failed to serialize log event: "
                    + e.getMessage() + "\"}" + System.lineSeparator();
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    private String buildBody(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        IThrowableProxy throwableProxy = event.getThrowableProxy();
        if (throwableProxy == null) {
            return message;
        }
        return message + System.lineSeparator() + ThrowableProxyUtil.asString(throwableProxy);
    }

    private Map<String, Object> buildAttributes(ILoggingEvent event) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        StackTraceElement[] callerData = event.getCallerData();
        if (callerData != null && callerData.length > 0) {
            StackTraceElement caller = callerData[0];
            attributes.put("code.filepath", caller.getFileName());
            attributes.put("code.lineno", caller.getLineNumber());
            attributes.put("code.function", caller.getMethodName());
        }
        attributes.put("logger.name", event.getLoggerName());

        // Preserve any other MDC entries (e.g. this project's existing "clientId" field) as
        // additional attributes, excluding the trace/span keys already promoted to top level.
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null) {
            mdc.forEach((key, value) -> {
                if (!MDC_TRACE_ID.equals(key) && !MDC_SPAN_ID.equals(key) && value != null) {
                    attributes.put(key, value);
                }
            });
        }
        return attributes;
    }

    private int mapSeverityNumber(ch.qos.logback.classic.Level level) {
        if (level == ch.qos.logback.classic.Level.TRACE) {
            return 1;
        } else if (level == ch.qos.logback.classic.Level.DEBUG) {
            return 5;
        } else if (level == ch.qos.logback.classic.Level.INFO) {
            return 9;
        } else if (level == ch.qos.logback.classic.Level.WARN) {
            return 13;
        } else if (level == ch.qos.logback.classic.Level.ERROR) {
            return 17;
        }
        return 9;
    }

    private String resolveServiceName() {
        String envName = System.getenv("OTEL_SERVICE_NAME");
        if (envName != null && !envName.isBlank()) {
            return envName;
        }
        return serviceName;
    }

    private String resolveServiceVersion() {
        if (serviceVersion != null && !serviceVersion.isBlank()) {
            return serviceVersion;
        }
        String implVersion = getClass().getPackage().getImplementationVersion();
        return implVersion != null ? implVersion : "unknown";
    }

    /** Setter used by logback.xml's appender configuration (Logback calls this via reflection). */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /** Setter used by logback.xml's appender configuration (Logback calls this via reflection). */
    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }
}


