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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.rest.app.properties.RestAppProperties;
import org.flowable.rest.security.BasicAuthenticationProvider;
import org.flowable.rest.security.PreAuthenticatedUserDetailsService;
import org.flowable.rest.security.SecurityConstants;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {

    protected static final String MODE_PRE_AUTH = "pre-auth";
    protected static final String MODE_VERIFY_PRIVILEGE = "verify-privilege";

    protected final RestAppProperties restAppProperties;

    public SecurityConfiguration(RestAppProperties restAppProperties) {
        this.restAppProperties = restAppProperties;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(IdmIdentityService idmIdentityService) {
        if (isPreAuth()) {
            // The reverse proxy has already authenticated the caller; this provider only loads
            // the user's privileges from IDM. No password is checked.
            PreAuthenticatedUserDetailsService userDetailsService = new PreAuthenticatedUserDetailsService(idmIdentityService);
            userDetailsService.setVerifyRestApiPrivilege(isVerifyRestApiPrivilege());

            PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
            provider.setPreAuthenticatedUserDetailsService(userDetailsService);
            return provider;
        }

        BasicAuthenticationProvider basicAuthenticationProvider = new BasicAuthenticationProvider();
        basicAuthenticationProvider.setVerifyRestApiPrivilege(isVerifyRestApiPrivilege());
        return basicAuthenticationProvider;
    }

    @Bean
    public SecurityFilterChain restApiSecurity(HttpSecurity http, AuthenticationProvider authenticationProvider) throws Exception {
        HttpSecurity httpSecurity = http.authenticationProvider(authenticationProvider)
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(CsrfConfigurer::disable);

        if (restAppProperties.getCors().isEnabled()) {
            httpSecurity.apply(new PropertyBasedCorsFilter(restAppProperties));
        }

        // Swagger docs
        if (isSwaggerDocsEnabled()) {
            httpSecurity
                    .authorizeHttpRequests(
                            authorizeRequests -> authorizeRequests.requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/docs/**")).permitAll());

        } else {
            httpSecurity
                    .authorizeHttpRequests(
                            authorizeRequests -> authorizeRequests.requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/docs/**")).denyAll());

        }

        httpSecurity
            .authorizeHttpRequests(authorizeRequests ->
                authorizeRequests
                        .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class)).authenticated()
                        .requestMatchers(EndpointRequest.toAnyEndpoint()).hasAnyAuthority(SecurityConstants.ACCESS_ADMIN)
            );


        // Rest API access
        if (isVerifyRestApiPrivilege()) {
            httpSecurity
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests.anyRequest().hasAuthority(SecurityConstants.PRIVILEGE_ACCESS_REST_API));

        } else {
            httpSecurity
            .authorizeHttpRequests(authorizeRequests -> authorizeRequests.anyRequest().authenticated());
        }

        if (isPreAuth()) {
            // Identity comes from a header set by a trusted proxy, not HTTP Basic. The filter
            // builds a PreAuthenticatedAuthenticationToken from the header, which the
            // PreAuthenticatedAuthenticationProvider above resolves against IDM.
            RequestHeaderAuthenticationFilter preAuthFilter = trustedProxyAware(
                    new RequestHeaderAuthenticationFilter());
            preAuthFilter.setPrincipalRequestHeader(restAppProperties.getPreAuth().getPrincipalHeader());
            // Missing header simply yields an anonymous request that the authorization rules
            // above reject with 401/403, rather than a 500.
            preAuthFilter.setExceptionIfHeaderMissing(false);
            preAuthFilter.setAuthenticationManager(authentication -> authenticationProvider.authenticate(authentication));
            httpSecurity.addFilterBefore(preAuthFilter, org.springframework.security.web.authentication.AnonymousAuthenticationFilter.class);
        } else {
            httpSecurity.httpBasic(Customizer.withDefaults());
        }

        return http.build();
    }

    protected boolean isVerifyRestApiPrivilege() {
        String authMode = restAppProperties.getAuthenticationMode();
        if (StringUtils.isNotEmpty(authMode)) {
            // 'pre-auth' keeps privilege verification on: identity is trusted, authorization is not.
            return MODE_VERIFY_PRIVILEGE.equals(authMode) || MODE_PRE_AUTH.equals(authMode);
        }
        return true; // checking privilege is the default
    }

    protected boolean isPreAuth() {
        return MODE_PRE_AUTH.equals(restAppProperties.getAuthenticationMode());
    }

    /**
     * Wraps the pre-auth filter so that, when a trusted-proxy allowlist is configured, the
     * principal header is only read from requests whose transport peer address matches the
     * allowlist. A request from any other source is treated as if it carried no header
     * (principal resolves to {@code null}) and is denied by the authorization rules, exactly
     * like a missing header. With no allowlist configured the plain filter is returned and
     * behaviour is unchanged.
     */
    protected RequestHeaderAuthenticationFilter trustedProxyAware(RequestHeaderAuthenticationFilter delegate) {
        List<String> trustedProxies = restAppProperties.getPreAuth().getTrustedProxies();
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            return delegate;
        }
        List<IpAddressMatcher> matchers = new ArrayList<>(trustedProxies.size());
        for (String entry : trustedProxies) {
            matchers.add(new IpAddressMatcher(entry));
        }
        return new RequestHeaderAuthenticationFilter() {

            @Override
            protected Object getPreAuthenticatedPrincipal(jakarta.servlet.http.HttpServletRequest request) {
                String remoteAddr = request.getRemoteAddr();
                for (IpAddressMatcher matcher : matchers) {
                    if (matcher.matches(remoteAddr)) {
                        return super.getPreAuthenticatedPrincipal(request);
                    }
                }
                // Untrusted source: ignore the header entirely.
                return null;
            }
        };
    }

    protected boolean isSwaggerDocsEnabled() {
        return restAppProperties.isSwaggerDocsEnabled();
    }
}
