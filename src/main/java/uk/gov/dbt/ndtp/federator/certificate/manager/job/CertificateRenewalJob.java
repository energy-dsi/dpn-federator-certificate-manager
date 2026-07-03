/*
 * SPDX-License-Identifier: Apache-2.0
 * © Crown Copyright 2026. This work has been developed by the National Digital Twin Programme and is legally
 * attributed to the Department for Business and Trade (UK) as the governing entity.
 */

package uk.gov.dbt.ndtp.federator.certificate.manager.job;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.dbt.ndtp.federator.certificate.manager.service.CertificateManagerService;

/**
 * Job responsible for periodically checking and renewing certificates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CertificateRenewalJob {

    private final CertificateManagerService certificateManagerService;
    private final ObservationRegistry observationRegistry;

    /**
     * Periodically executed task to check certificate status and initiate renewal if necessary.
     */
    @Scheduled(
            fixedDelayString = "${application.scheduling.certificate-manager.renewal-rate}",
            initialDelayString = "${application.scheduling.certificate-manager.renewal-initial-delay:10000}")
    public void execute() {
        log.debug("Executing Certificate Renewal Job");
        // Observation.observe(...) creates a span (via the OTel bridge, see pom.xml/application.yml)
        // around the exact same call below; trace_id/span_id are then populated into MDC for
        // every log line emitted inside certificateManagerService.run() - no changes needed
        // there. The original try/catch is preserved unchanged inside the observed block, so
        // error handling behaviour is identical to before this change.
        Observation.createNotStarted("certificate.renewal", observationRegistry).observe(() -> {
            try {
                certificateManagerService.run();
            } catch (Exception e) {
                log.error("Error during certificate renewal job execution: {}", e.getMessage(), e);
            }
        });
    }
}
