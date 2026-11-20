package com.example.assignment.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.apache.commons.logging.Log;

import java.util.Arrays;
import java.util.Locale;

/** Reports safe datasource URL metadata after config data is loaded and before datasource auto-configuration. */
public class DatasourceUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private final Log logger;

    public DatasourceUrlEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.logger = logFactory.getLog(getClass());
    }

    @Override
    public int getOrder() {
        // ConfigDataEnvironmentPostProcessor runs at higher precedence, loading application-{profile}.yml first.
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String effectiveUrl = environment.getProperty("spring.datasource.url");
        boolean springDatasourceUrlExists = System.getenv().containsKey("SPRING_DATASOURCE_URL");
        boolean datasourcePropertyExists = environment.containsProperty("spring.datasource.url");
        boolean startsWithJdbc = effectiveUrl != null && effectiveUrl.startsWith("jdbc:");
        String source = datasourcePropertySource(environment);
        String[] activeProfiles = environment.getActiveProfiles();

        logger.info(String.format(Locale.ROOT,
                "Datasource startup diagnostic: SPRING_DATASOURCE_URL exists=%s, " +
                        "spring.datasource.url exists=%s, startsWithJdbc=%s, prefix=%s, " +
                        "propertySource=%s, activeProfiles=%s",
                springDatasourceUrlExists,
                datasourcePropertyExists,
                startsWithJdbc,
                safePrefix(effectiveUrl),
                source,
                activeProfiles.length == 0 ? "<none>" : Arrays.toString(activeProfiles)));
    }

    private String datasourcePropertySource(ConfigurableEnvironment environment) {
        for (PropertySource<?> propertySource : environment.getPropertySources()) {
            // This adapter aggregates the concrete sources below it; report the concrete source instead.
            if ("configurationProperties".equals(propertySource.getName())) continue;
            if (propertySource.getProperty("spring.datasource.url") != null) return propertySource.getName();
        }
        return "<none>";
    }

    private String safePrefix(String url) {
        if (url == null || url.isBlank()) return "<empty>";
        String normalized = url.strip().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("jdbc:")) {
            int driverEnd = normalized.indexOf(':', "jdbc:".length());
            return driverEnd > "jdbc:".length()
                    ? normalized.substring(0, driverEnd + 1)
                    : "jdbc:<incomplete>";
        }
        int colon = normalized.indexOf(':');
        if (colon > 0 && normalized.substring(0, colon).matches("[a-z][a-z0-9+.-]*")) {
            return normalized.substring(0, colon + 1);
        }
        return "<no-scheme>";
    }
}
