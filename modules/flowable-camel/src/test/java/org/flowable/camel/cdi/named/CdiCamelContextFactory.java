package org.flowable.camel.cdi.named;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

public class CdiCamelContextFactory {

    @Produces
    @ApplicationScoped
    @Named("camelContext")
    CamelContext createDefaultContext() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        return camelContext;
    }

    @Produces
    @ApplicationScoped
    @Named("myContext")
    CamelContext createMyCamelContext() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        return camelContext;
    }

}
