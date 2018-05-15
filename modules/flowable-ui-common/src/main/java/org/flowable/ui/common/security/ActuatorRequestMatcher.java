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

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPathProvider;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * A {@link RequestMatcher} that is matches all endpoints of an actuator. It should be used when creating a request matcher when configuring Spring Security.
 * This class is needed as there is no such support from Spring Boot at this moment.
 *
 * @author Filip Hrisafov
 */
public class ActuatorRequestMatcher extends ApplicationContextRequestMatcher<WebApplicationContext> {

    private static final RequestMatcher EMPTY_MATCHER = (request) -> false;

    private volatile RequestMatcher delegate;

    public ActuatorRequestMatcher() {
        super(WebApplicationContext.class);
    }

    @Override
    protected final void initialized(Supplier<WebApplicationContext> context) {
        this.delegate = createDelegate(context.get());
    }

    @Override
    protected final boolean matches(HttpServletRequest request,
        Supplier<WebApplicationContext> context) {
        return this.delegate.matches(request);
    }

    private RequestMatcher createDelegate(WebApplicationContext context) {
        try {
            RequestMatcherFactory requestMatcherFactory = new RequestMatcherFactory(
                context.getBean(DispatcherServletPathProvider.class)
                    .getServletPath());
            return createDelegate(context, requestMatcherFactory);
        } catch (NoSuchBeanDefinitionException ex) {
            return EMPTY_MATCHER;
        }
    }

    protected RequestMatcher createDelegate(WebApplicationContext context, RequestMatcherFactory requestMatcherFactory) {
        WebEndpointProperties properties = context.getBean(WebEndpointProperties.class);
        if (StringUtils.hasText(properties.getBasePath())) {
            return requestMatcherFactory.antPath(properties.getBasePath() + "/**");
        }
        return EMPTY_MATCHER;
    }

    /**
     * Factory used to create a {@link RequestMatcher}.
     */
    private static class RequestMatcherFactory {

        private final String servletPath;

        RequestMatcherFactory(String servletPath) {
            this.servletPath = servletPath;
        }

        public RequestMatcher antPath(String part) {
            String pattern = (this.servletPath.equals("/") ? "" : this.servletPath);
            return new AntPathRequestMatcher(pattern + part);
        }

    }
}
