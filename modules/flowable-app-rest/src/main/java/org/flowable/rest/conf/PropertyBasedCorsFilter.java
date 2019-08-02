package org.flowable.rest.conf;

import org.flowable.rest.app.properties.RestAppProperties;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configures Cross-Origin Resource Sharing (CORS) based on injected properties.
 *
 * @author Tim Stephenson
 */
public class PropertyBasedCorsFilter extends AbstractHttpConfigurer<PropertyBasedCorsFilter, HttpSecurity> {
    
    protected final RestAppProperties restAppProperties;

    public PropertyBasedCorsFilter(RestAppProperties restAppProperties) {
        this.restAppProperties = restAppProperties;
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        CorsFilter corsFilter = corsFilter(restAppProperties);
        http.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class);
    }

    private CorsFilter corsFilter(RestAppProperties restAppProperties) {
        CorsConfiguration config = new CorsConfiguration();
        if (restAppProperties.isCorsAllowCredentials()) {
            config.setAllowCredentials(true);
        }

        for (String origin : restAppProperties.getCorsAllowedOrigins()) {
            config.addAllowedOrigin(origin);
        }
        for (String header : restAppProperties.getCorsAllowedHeaders()) {
            config.addAllowedHeader(header);
        }
        for (String exposedHeader : restAppProperties.getCorsExposedHeaders()) {
            config.addExposedHeader(exposedHeader);
        }
        for (String method : restAppProperties.getCorsAllowedMethods()) {
            config.addAllowedMethod(method);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}