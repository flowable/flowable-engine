package org.flowable.rest.conf;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

@Configuration
@PropertySources({
    
    @PropertySource("classpath:/META-INF/flowable-app/flowable-app.properties"),
    @PropertySource(value = "classpath:flowable-app.properties", ignoreResourceNotFound = true),
    @PropertySource(value = "file:flowable-app.properties", ignoreResourceNotFound = true),
    
    // For backwards compatibility (pre 6.3.0)
    @PropertySource(value = "classpath:db.properties", ignoreResourceNotFound = true),
    @PropertySource(value = "classpath:engine.properties", ignoreResourceNotFound = true)
        
})
@ComponentScan(basePackages = { "org.flowable.rest.conf" })
@ImportResource({ "classpath:flowable-custom-context.xml" })
public class ApplicationConfiguration {

    /**
     * This is needed to make property resolving work on annotations ... (see http://stackoverflow.com/questions/11925952/custom-spring-property-source-does-not-resolve-placeholders-in-value)
     *
     * @Scheduled(cron="${someProperty}")
     */
    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public static PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertyPlaceholderConfigurer placeholderConfigurer = new PropertyPlaceholderConfigurer();
        placeholderConfigurer.setSystemPropertiesMode(PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_OVERRIDE);
        return placeholderConfigurer;
    }

}
