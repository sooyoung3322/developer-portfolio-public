package com.portfolio.backend;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TestConfigurationIsolationTests {
    @Test void runtimeUsesCommittedResourcesWithoutDeveloperLocalSettings() {
        var resources = getClass().getClassLoader();
        for (String extension : new String[]{"yml", "yaml", "properties"}) {
            assertThat(resources.getResource("application-local." + extension))
                .as("developer-only application-local.%s must not enter test runtime", extension).isNull();
        }
        for (String name : new String[]{"application.yml", "application-prod.yml",
                "db/migration/V5__analytics_session_baseline.sql"}) {
            assertThat(resources.getResource(name)).as("committed test resource %s", name).isNotNull();
        }
    }
}
