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
import java.util.Collections;
import java.util.LinkedHashMap;

import org.flowable.ui.common.properties.FlowableCommonAppProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.PortMapper;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.DelegatingAuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ClassUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

/**
 * @author Filip Hrisafov
 * @see org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer
 */
public class FlowableUiCustomFormLoginConfigurer<H extends HttpSecurityBuilder<H>> extends
        AbstractHttpConfigurer<FlowableUiCustomFormLoginConfigurer<H>, H> {

    protected UsernamePasswordAuthenticationFilter authenticationFilter;
    protected LoginUrlAuthenticationEntryPoint authenticationEntryPoint;

    public FlowableUiCustomFormLoginConfigurer() {
        this.authenticationFilter = new UsernamePasswordAuthenticationFilter();
        this.authenticationFilter.setUsernameParameter("j_username");
        this.authenticationFilter.setPasswordParameter("j_password");
        this.authenticationFilter.setAuthenticationSuccessHandler(new AjaxAuthenticationSuccessHandler());
        this.authenticationFilter.setAuthenticationFailureHandler(new AjaxAuthenticationFailureHandler());
        this.authenticationFilter.setRequiresAuthenticationRequestMatcher(new AntPathRequestMatcher("/app/authentication", "POST"));
    }

    @Override
    public void init(H builder) throws Exception {
        super.init(builder);

        AuthenticationEntryPoint authenticationEntryPoint = getAuthenticationEntryPoint(builder.getSharedObject(ApplicationContext.class));
        ExceptionHandlingConfigurer<H> exceptionHandling = builder.getConfigurer(ExceptionHandlingConfigurer.class);
        if (exceptionHandling != null) {
            LinkedHashMap<RequestMatcher, AuthenticationEntryPoint> entryPoints = new LinkedHashMap<>();
            entryPoints.put(getAuthenticationEntryPointMatcher(builder), postProcess(authenticationEntryPoint));
            DelegatingAuthenticationEntryPoint delegatingAuthenticationEntryPoint = new DelegatingAuthenticationEntryPoint(entryPoints);
            delegatingAuthenticationEntryPoint.setDefaultEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));
            exceptionHandling.authenticationEntryPoint(delegatingAuthenticationEntryPoint);
            exceptionHandling.addObjectPostProcessor(new ObjectPostProcessor<ExceptionTranslationFilter>() {

                @Override
                public <O extends ExceptionTranslationFilter> O postProcess(O object) {
                    object.setAuthenticationTrustResolver(new FlowableAuthenticationTrustResolver());
                    return object;
                }
            });
        }
    }

    protected RequestMatcher getAuthenticationEntryPointMatcher(H http) {
        ContentNegotiationStrategy contentNegotiationStrategy = http.getSharedObject(ContentNegotiationStrategy.class);
        if (contentNegotiationStrategy == null) {
            contentNegotiationStrategy = new HeaderContentNegotiationStrategy();
        }

        MediaTypeRequestMatcher mediaMatcher = new MediaTypeRequestMatcher(contentNegotiationStrategy, MediaType.APPLICATION_XHTML_XML,
                new MediaType("image", "*"), MediaType.TEXT_HTML, MediaType.TEXT_PLAIN);
        mediaMatcher.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));

        RequestMatcher notXRequestedWith = new NegatedRequestMatcher(new RequestHeaderRequestMatcher("X-Requested-With", "XMLHttpRequest"));

        return new AndRequestMatcher(Arrays.asList(notXRequestedWith, mediaMatcher));
    }

    protected LoginUrlAuthenticationEntryPoint getAuthenticationEntryPoint(ApplicationContext applicationContext) {
        if (this.authenticationEntryPoint == null) {
            FlowableCommonAppProperties commonAppProperties = applicationContext.getBean(FlowableCommonAppProperties.class);
            if (ClassUtils.isPresent("org.flowable.ui.idm.service.GroupServiceImpl", getClass().getClassLoader())) {
                // If the groups service (idm app) is present then it should be redirected to it automatically)
                this.authenticationEntryPoint = postProcess(new LoginUrlAuthenticationEntryPoint("/idm/#/login"));
            } else {
                String redirectOnAuthSuccess = commonAppProperties.getRedirectOnAuthSuccess();
                String idmAppUrl = commonAppProperties.determineIdmAppRedirectUrl();
                this.authenticationEntryPoint = postProcess(new FlowableLoginUrlAuthenticationEntryPoint(idmAppUrl, redirectOnAuthSuccess));
            }
        }

        return this.authenticationEntryPoint;
    }

    @Override
    public void configure(H builder) throws Exception {
        PortMapper portMapper = builder.getSharedObject(PortMapper.class);
        if (portMapper != null) {
            getAuthenticationEntryPoint(builder.getSharedObject(ApplicationContext.class)).setPortMapper(portMapper);
        }
        authenticationFilter.setAuthenticationManager(builder.getSharedObject(AuthenticationManager.class));

        SessionAuthenticationStrategy sessionAuthenticationStrategy = builder.getSharedObject(SessionAuthenticationStrategy.class);
        if (sessionAuthenticationStrategy != null) {
            authenticationFilter.setSessionAuthenticationStrategy(sessionAuthenticationStrategy);
        }

        RememberMeServices rememberMeServices = builder.getSharedObject(RememberMeServices.class);
        if (rememberMeServices != null) {
            authenticationFilter.setRememberMeServices(rememberMeServices);
        }

        builder.addFilter(postProcess(authenticationFilter));
    }
}
