package org.flowable.dmn.rest.conf.common;

import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.common.rest.resolver.DefaultContentTypeResolver;
import org.flowable.dmn.rest.service.api.DmnRestResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yvo Swillens
 */
@Configuration
public class RestConfiguration {

    @Bean()
    public DmnRestResponseFactory restDmnResponseFactory() {
        DmnRestResponseFactory restResponseFactory = new DmnRestResponseFactory();
        return restResponseFactory;
    }

    @Bean()
    public ContentTypeResolver contentTypeResolver() {
        ContentTypeResolver resolver = new DefaultContentTypeResolver();
        return resolver;
    }
}
