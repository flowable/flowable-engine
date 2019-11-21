/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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