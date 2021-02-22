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
package org.flowable.ui.common.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.flowable.ui.common.rest.idm.CurrentUserProvider;
import org.flowable.ui.common.rest.idm.OAuth2CurrentUserProvider;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.flowable.ui.common.service.idm.RemoteIdmServiceImpl;
import org.flowable.ui.common.service.idm.cache.RemoteIdmUserCache;
import org.flowable.ui.common.service.idm.cache.UserCache;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

/**
 * @author Filip Hrisafov
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({
        IdmEngineServicesAutoConfiguration.class,
})
@AutoConfigureBefore({
        FlowableSecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
})
public class FlowableUiSecurityAutoConfiguration {

    private static final Customizer<ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry> DEFAULT_AUTHORIZE_REQUESTS = requests -> {
        requests.antMatchers("/app/rest/account").authenticated()
                .antMatchers("/app/rest/runtime/app-definitions").authenticated()
                .antMatchers("/idm-app/rest/authenticate").authenticated()
                .antMatchers("/idm-app/rest/account").authenticated()
                .antMatchers("/app/rest/**", "/workflow/").hasAuthority(DefaultPrivileges.ACCESS_TASK)
                .antMatchers("/admin-app/**", "/admin/").hasAuthority(DefaultPrivileges.ACCESS_ADMIN)
                .antMatchers("/idm-app/**").hasAuthority(DefaultPrivileges.ACCESS_IDM)
                .antMatchers("/modeler-app/**", "/modeler/").hasAuthority(DefaultPrivileges.ACCESS_MODELER)
                .antMatchers("/").authenticated()
                .antMatchers("/app/authentication").permitAll()
                .antMatchers("/idm").permitAll();
    };

    private static final Customizer<LogoutConfigurer<HttpSecurity>> DEFAULT_LOGOUT = logout -> {
        logout.logoutUrl("/app/logout");
    };

    private static final Customizer<HeadersConfigurer<HttpSecurity>> DEFAULT_HEADERS = headers -> {
        headers.frameOptions()
                .sameOrigin()
                .addHeaderWriter(new XXssProtectionHeaderWriter());
    };

    public FlowableUiSecurityAutoConfiguration(ObjectProvider<SecurityScopeProvider> securityScopeProvider) {
        // Override the default security scope provider if there is such bean
        SecurityUtils.setSecurityScopeProvider(securityScopeProvider.getIfAvailable(FlowableSecurityScopeProvider::new));
    }

    @Bean
    @ConditionalOnMissingBean
    public RememberMeServices flowableUiRememberMeService(FlowableCommonAppProperties properties, UserDetailsService userDetailsService,
            PersistentTokenService persistentTokenService) {
        return new CustomPersistentRememberMeServices(properties, userDetailsService, persistentTokenService);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.flowable.ui.idm.service.GroupServiceImpl")
    public static class RemoteIdmConfiguration {
        // This configuration is used when the idm application is not part of the UI application

        @Bean
        public RemoteIdmService remoteIdmService(FlowableCommonAppProperties properties) {
            return new RemoteIdmServiceImpl(properties);
        }

        @Bean
        public UserCache remoteIdmUserCache(FlowableCommonAppProperties properties, RemoteIdmService remoteIdmService) {
            return new RemoteIdmUserCache(properties, remoteIdmService);
        }

        @Bean
        @ConditionalOnMissingBean
        public UserDetailsService flowableUiUserDetailsService(RemoteIdmService remoteIdmService) {
            return new RemoteIdmUserDetailsService(remoteIdmService);
        }

        @Bean
        @ConditionalOnMissingBean
        public PersistentTokenService flowableUiPersistentTokenService(RemoteIdmService remoteIdmService) {
            return new RemoteIdmPersistentTokenService(remoteIdmService);
        }

        @Bean
        public RemoteIdmAuthenticationProvider remoteIdmAuthenticationProvider(RemoteIdmService remoteIdmService) {
            return new RemoteIdmAuthenticationProvider(remoteIdmService);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.flowable.ui.idm.service.GroupServiceImpl")
    public static class LocalIdmConfiguration {
        // This configuration is used when the idm application is part of the UI application

        @Bean
        @ConditionalOnMissingBean
        public PersistentTokenService flowableUiPersistentTokenService(IdmIdentityService idmIdentityService) {
            return new IdmEnginePersistentTokenService(idmIdentityService);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Order(SecurityConstants.FORM_LOGIN_SECURITY_ORDER)
    @ConditionalOnProperty(prefix = "flowable.common.app.security", name = "type", havingValue = "idm", matchIfMissing = true)
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        protected ObjectProvider<RememberMeServices> rememberMeServicesObjectProvider;

        @Autowired
        protected FlowableCommonAppProperties commonAppProperties;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            RememberMeServices rememberMeServices = rememberMeServicesObjectProvider.getIfAvailable();
            String key = null;
            if (rememberMeServices instanceof AbstractRememberMeServices) {
                key = ((AbstractRememberMeServices) rememberMeServices).getKey();
            }
            if (rememberMeServices != null) {
                http.rememberMe()
                        .key(key)
                        .rememberMeServices(rememberMeServices);
            }
            http
                    .exceptionHandling()
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .logout(logout -> {
                        DEFAULT_LOGOUT.customize(logout);
                        logout.logoutSuccessUrl("/");
                        logout.addLogoutHandler(new ClearFlowableCookieLogoutHandler());
                    })
                    .csrf()
                    .disable() // Disabled, cause enabling it will cause sessions
                    .headers(DEFAULT_HEADERS)
                    // Never persist the security context
                    .securityContext().securityContextRepository(new NullSecurityContextRepository())
                    .and()
                    .authorizeRequests(DEFAULT_AUTHORIZE_REQUESTS)
            ;

            http.formLogin().disable();
            http.apply(new FlowableUiCustomFormLoginConfigurer<>());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Order(SecurityConstants.FORM_LOGIN_SECURITY_ORDER)
    @ConditionalOnProperty(prefix = "flowable.common.app.security", name = "type", havingValue = "oauth2")
    public static class OAuthWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");

        protected final FlowableCommonAppProperties commonAppProperties;
        protected final ObjectProvider<PasswordEncoder> passwordEncoder;

        public OAuthWebSecurityConfigurerAdapter(FlowableCommonAppProperties commonAppProperties,
                ObjectProvider<PasswordEncoder> passwordEncoder) {
            this.commonAppProperties = commonAppProperties;
            this.passwordEncoder = passwordEncoder;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            // Do not disable session, otherwise OAuth does not work
            http
                    .logout(logout -> {
                        DEFAULT_LOGOUT.customize(logout);
                        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(
                                getApplicationContext().getBean(ClientRegistrationRepository.class));
                        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}");
                        logout.logoutSuccessHandler(logoutSuccessHandler);
                    })
                    .csrf()
                    .disable() // Disabled, cause enabling it will cause sessions
                    .headers(DEFAULT_HEADERS)
                    .authorizeRequests(DEFAULT_AUTHORIZE_REQUESTS);

            http.oauth2Login();
            http.oauth2Client();
        }

        @Bean
        public GrantedAuthoritiesMapper keycloakAuthoritiesMapper() {
            FlowableCommonAppProperties.OAuth2 oAuth2 = commonAppProperties.getSecurity().getOAuth2();
            String authoritiesAttribute = oAuth2.getAuthoritiesAttribute();
            String groupsAttribute = oAuth2.getGroupsAttribute();
            Collection<String> defaultAuthorities = oAuth2.getDefaultAuthorities();
            Collection<String> defaultGroups = oAuth2.getDefaultGroups();
            return new FlowableOAuth2GrantedAuthoritiesMapper(authoritiesAttribute, groupsAttribute, defaultAuthorities, defaultGroups);
        }

        @Bean
        @ConditionalOnClass(Jwt.class)
        @ConditionalOnMissingBean
        public ApiHttpSecurityCustomizer delegatingApiHttpSecurityCustomizer() {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            FlowableCommonAppProperties.OAuth2 oAuth2 = commonAppProperties.getSecurity().getOAuth2();
            String authoritiesAttribute = oAuth2.getAuthoritiesAttribute();
            String groupsAttribute = oAuth2.getGroupsAttribute();
            Collection<String> defaultAuthorities = oAuth2.getDefaultAuthorities();
            Collection<String> defaultGroups = oAuth2.getDefaultGroups();
            converter.setJwtGrantedAuthoritiesConverter(new FlowableJwtGrantedAuthoritiesMapper(authoritiesAttribute, groupsAttribute, defaultAuthorities, defaultGroups));

            JwtApiHttpSecurityCustomizer jwtApiHttpSecurityCustomizer = new JwtApiHttpSecurityCustomizer(converter);

            String username = commonAppProperties.getIdmAdmin().getUser();
            String password = commonAppProperties.getIdmAdmin().getPassword();
            FixUserApiHttpSecurityCustomizer fixUserApiHttpSecurityCustomizer = new FixUserApiHttpSecurityCustomizer(username, deducePassword(password));
            return new DelegatingApiHttpSecurityCustomizer(
                    Arrays.asList(
                            fixUserApiHttpSecurityCustomizer,
                            jwtApiHttpSecurityCustomizer
                    )
            );
        }

        @Bean
        @ConditionalOnMissingClass("org.springframework.security.oauth2.jwt.Jwt")
        @ConditionalOnMissingBean
        public ApiHttpSecurityCustomizer fixUserApiHttpSecurityCustomizer() {
            String username = commonAppProperties.getIdmAdmin().getUser();
            String password = commonAppProperties.getIdmAdmin().getPassword();
            return new FixUserApiHttpSecurityCustomizer(username, deducePassword(password));
        }

        @Bean
        @ConditionalOnMissingBean(name = "oauth2CurrentUserProvider")
        public CurrentUserProvider oauth2CurrentUserProvider() {
            FlowableCommonAppProperties.OAuth2 oAuth2 = commonAppProperties.getSecurity().getOAuth2();
            OAuth2CurrentUserProvider currentUserProvider = new OAuth2CurrentUserProvider();

            PropertyMapper mapper = PropertyMapper.get().alwaysApplyingWhenNonNull();
            mapper.from(oAuth2.getFirstNameAttribute()).to(currentUserProvider::setFirstNameKey);
            mapper.from(oAuth2.getLastNameAttribute()).to(currentUserProvider::setLastNameKey);
            mapper.from(oAuth2.getFullNameAttribute()).to(currentUserProvider::setFullNameKey);
            mapper.from(oAuth2.getEmailAttribute()).to(currentUserProvider::setEmailKey);

            return currentUserProvider;
        }

        protected String deducePassword(String password) {
            PasswordEncoder passwordEncoder = this.passwordEncoder.getIfAvailable();
            if (PASSWORD_ALGORITHM_PATTERN.matcher(password).matches()) {
                return password;
            }

            if (passwordEncoder != null) {
                return passwordEncoder.encode(password);
            }

            return password;
        }
    }

    //
    // Actuator
    //

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(EndpointRequest.class)
    @Order(SecurityConstants.ACTUATOR_SECURITY_ORDER)
    public static class ActuatorWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        protected final ApiHttpSecurityCustomizer apiHttpSecurityCustomizer;

        public ActuatorWebSecurityConfigurationAdapter(ApiHttpSecurityCustomizer apiHttpSecurityCustomizer) {
            this.apiHttpSecurityCustomizer = apiHttpSecurityCustomizer;
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {

            http
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .csrf()
                    .disable();

            http
                    .requestMatcher(new ActuatorRequestMatcher())
                    .authorizeRequests()
                    .requestMatchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class)).authenticated()
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).hasAnyAuthority(DefaultPrivileges.ACCESS_ADMIN);

            apiHttpSecurityCustomizer.customize(http);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "flowable.common.app.security", name = "type", havingValue = "idm", matchIfMissing = true)
    public ApiHttpSecurityCustomizer defaultApiHttpSecurityCustomizer() {
        return new DefaultApiHttpSecurityCustomizer();
    }
}
