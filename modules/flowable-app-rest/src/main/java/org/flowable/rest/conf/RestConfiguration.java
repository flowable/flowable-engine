package org.flowable.rest.conf;

import org.flowable.rest.application.ContentTypeResolver;
import org.flowable.rest.application.DefaultContentTypeResolver;
import org.flowable.rest.content.ContentRestResponseFactory;
import org.flowable.rest.dmn.service.api.DmnRestResponseFactory;
import org.flowable.rest.form.FormRestResponseFactory;
import org.flowable.rest.idm.service.api.IdmRestResponseFactory;
import org.flowable.rest.service.api.RestResponseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Joram Barrez
 * @author Yvo Swillens
 */
@Configuration
public class RestConfiguration {

    @Bean
    public ContentTypeResolver contentTypeResolver() {
        ContentTypeResolver resolver = new DefaultContentTypeResolver();
        return resolver;
    }

    @Bean
    public RestResponseFactory processResponseFactory() {
        return new RestResponseFactory();
    }
    
    @Bean
    public IdmRestResponseFactory idmRestResponseFactory() {
        return new IdmRestResponseFactory();
    }

    @Bean
    public DmnRestResponseFactory dmnResponseFactory() {
        return new DmnRestResponseFactory();
    }

    @Bean
    public FormRestResponseFactory formResponseFactory() {
        return new FormRestResponseFactory();
    }

    @Bean
    public ContentRestResponseFactory contentResponseFactory() {
        return new ContentRestResponseFactory();
    }
}
