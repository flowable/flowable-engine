package org.flowable.rest.conf;

import org.flowable.rest.app.properties.RestAppProperties;
import org.springframework.boot.context.properties.PropertyMapper;
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
        CorsFilter corsFilter = corsFilter(restAppProperties.getCors());
        http.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class);
    }

    protected CorsFilter corsFilter(RestAppProperties.Cors cors) {
        CorsConfiguration config = new CorsConfiguration();
        if (cors.isAllowCredentials()) {
            config.setAllowCredentials(true);
        }

        for (String origin : cors.getAllowedOrigins()) {
            config.addAllowedOrigin(origin);
        }
        for (String header : cors.getAllowedHeaders()) {
            config.addAllowedHeader(header);
        }
        for (String exposedHeader : cors.getExposedHeaders()) {
            config.addExposedHeader(exposedHeader);
        }
        for (String method : cors.getAllowedMethods()) {
            config.addAllowedMethod(method);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}