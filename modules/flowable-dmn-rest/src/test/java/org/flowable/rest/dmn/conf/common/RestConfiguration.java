package org.flowable.rest.dmn.conf.common;

import org.flowable.rest.application.ContentTypeResolver;
import org.flowable.rest.application.DefaultContentTypeResolver;
import org.flowable.rest.dmn.service.api.DmnRestResponseFactory;
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
