package uk.gov.dbt.ndtp.federator.certificate.manager.config.logging;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class OpenTelemetryLogsConfig {

    @Value("${otel.logs.endpoint:http://localhost:4318/v1/logs}")
    private String endpoint;

    @Value("${management.observations.key-values.service.name:certificate-manager}")
    private String serviceName;

    @Bean
    public LoggerProvider sdkLoggerProvider() {

        // component.name is stamped as a resource attribute so the health-monitoring Data Prepper
        // populates log.component for every record from this service (it scans resource.attributes
        // for component.name), letting the dashboard group this service under one component. Kept
        // equal to service.name: certificate-manager is a single component with no finer-grained
        // sub-parts (mirrors the federator's component.name == service.name choice).
        Resource resource = Resource.getDefault()
                .merge(
                        Resource.create(
                                Attributes.of(
                                        AttributeKey.stringKey("service.name"),
                                        serviceName,
                                        AttributeKey.stringKey("component.name"),
                                        serviceName
                                )));

        OtlpHttpLogRecordExporter exporter =
                OtlpHttpLogRecordExporter.builder()
                        .setEndpoint(endpoint)
                        .build();

        SdkLoggerProvider loggerProvider =
                SdkLoggerProvider.builder()
                        .addLogRecordProcessor(
                                BatchLogRecordProcessor.builder(exporter)
                                        .build())
                        .setResource(resource)
                        .build();

        OpenTelemetrySdk openTelemetrySdk =
                OpenTelemetrySdk.builder()
                        .setLoggerProvider(loggerProvider)
                        .build();

        // Connect the Logback OpenTelemetryAppender (declared in logback-spring.xml) to this SDK.
        // Without this call the appender keeps its default OpenTelemetry.noop() instance and
        // silently drops every log record, which is why traces reached the Collector but logs
        // never did. Logs emitted before this point are buffered by the appender and replayed on
        // install. Uses build() rather than buildAndRegisterGlobal() so devtools restarts and
        // repeated test contexts do not fail with GlobalOpenTelemetry already-set errors.
        OpenTelemetryAppender.install(openTelemetrySdk);

        return loggerProvider;
    }
}