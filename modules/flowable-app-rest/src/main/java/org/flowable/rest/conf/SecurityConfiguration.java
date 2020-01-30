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

import org.apache.commons.lang3.StringUtils;
import org.flowable.rest.app.properties.RestAppProperties;
import org.flowable.rest.security.BasicAuthenticationProvider;
import org.flowable.rest.security.SecurityConstants;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {
    
    protected final RestAppProperties restAppProperties;

    public SecurityConfiguration(RestAppProperties restAppProperties) {
        this.restAppProperties = restAppProperties;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        BasicAuthenticationProvider basicAuthenticationProvider = new BasicAuthenticationProvider();
        basicAuthenticationProvider.setVerifyRestApiPrivilege(isVerifyRestApiPrivilege());
        return basicAuthenticationProvider;
    }
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        HttpSecurity httpSecurity = http.authenticationProvider(authenticationProvider())
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf().disable();

        if (restAppProperties.getCors().isEnabled()) {
            httpSecurity.apply(new PropertyBasedCorsFilter(restAppProperties));
        }

        // Swagger docs
        if (isSwaggerDocsEnabled()) {
            httpSecurity
                .authorizeRequests()
                .antMatchers("/docs/**").permitAll();

        } else {
            httpSecurity
                .authorizeRequests()
                .antMatchers("/docs/**").denyAll();
            
        }

        httpSecurity
            .authorizeRequests()
            .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class)).authenticated()
            .requestMatchers(EndpointRequest.toAnyEndpoint()).hasAnyAuthority(SecurityConstants.ACCESS_ADMIN);

        // Rest API access
        if (isVerifyRestApiPrivilege()) {
            httpSecurity
                .authorizeRequests()
                .anyRequest()
                .hasAuthority(SecurityConstants.PRIVILEGE_ACCESS_REST_API).and ().httpBasic();
            
        } else {
            httpSecurity
            .authorizeRequests()
            .anyRequest()
            .authenticated().and().httpBasic();
        }
    }
    
    protected boolean isVerifyRestApiPrivilege() {
        String authMode = restAppProperties.getAuthenticationMode();
        if (StringUtils.isNotEmpty(authMode)) {
            return "verify-privilege".equals(authMode);
        }
        return true; // checking privilege is the default
    }
    
    protected boolean isSwaggerDocsEnabled() {
        return restAppProperties.isSwaggerDocsEnabled();
    }
}
