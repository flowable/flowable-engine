package org.flowable.rest.conf;

import org.flowable.rest.application.ContentTypeResolver;
import org.flowable.rest.application.DefaultContentTypeResolver;
import org.flowable.rest.content.ContentRestResponseFactory;
import org.flowable.rest.dmn.service.api.DmnRestResponseFactory;
import org.flowable.rest.form.FormRestResponseFactory;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joram Barrez
 * @author Yvo Swillens
 */
@Configuration
public class RestConfiguration {

    @Bean()
    public ContentTypeResolver contentTypeResolver() {
        ContentTypeResolver resolver = new DefaultContentTypeResolver();
        return resolver;
    }

    @Bean()
    public RestResponseFactory processResponseFactory() {
        RestResponseFactory restResponseFactory = new RestResponseFactory();
        return restResponseFactory;
    }

    @Bean()
    public DmnRestResponseFactory dmnResponseFactory() {
        DmnRestResponseFactory restResponseFactory = new DmnRestResponseFactory();
        return restResponseFactory;
    }

    @Bean()
    public FormRestResponseFactory formResponseFactory() {
        FormRestResponseFactory restResponseFactory = new FormRestResponseFactory();
        return restResponseFactory;
    }

    @Bean()
    public ContentRestResponseFactory contentResponseFactory() {
        ContentRestResponseFactory restResponseFactory = new ContentRestResponseFactory();
        return restResponseFactory;
    }
}
