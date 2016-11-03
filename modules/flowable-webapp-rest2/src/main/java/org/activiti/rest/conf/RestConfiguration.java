package org.activiti.rest.conf;

import org.activiti.rest.application.ContentTypeResolver;
import org.activiti.rest.application.DefaultContentTypeResolver;
import org.activiti.rest.dmn.service.api.DmnRestResponseFactory;
import org.activiti.rest.form.FormRestResponseFactory;
import org.activiti.rest.service.api.RestResponseFactory;
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

}
