package com.example.assignment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Logs a credential-safe summary of the effective datasource URL before datasource initialization. */
@Component
public class DatasourceUrlStartupDiagnostic implements BeanFactoryPostProcessor, EnvironmentAware {
    private static final Logger logger = LoggerFactory.getLogger(DatasourceUrlStartupDiagnostic.class);
    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(
            org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String url = environment.getProperty("spring.datasource.url");
        boolean startsWithJdbc = url != null && url.startsWith("jdbc:");
        logger.info("Datasource URL diagnostic: startsWithJdbc={}, prefix={}", startsWithJdbc, safePrefix(url));
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
