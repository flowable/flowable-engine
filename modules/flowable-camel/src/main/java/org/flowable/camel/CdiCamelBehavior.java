package org.flowable.camel;

import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.flowable.cdi.impl.util.BeanManagerLookup;
import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * Camel CDI's default context is not named, so if the contextName being asked for is the same as the default and no such bean name is registered it will attempt to look up the default non-named 
 * CamelContext.
 * 
 * Creating a named context can be done by registering a class like this with CDI:
 * <pre>
 * {@code
 * 	import javax.enterprise.context.ApplicationScoped;
 *	import javax.enterprise.inject.Produces;
 *	import javax.inject.Named;
 *	
 *	import org.apache.camel.CamelContext;
 *	import org.apache.camel.impl.DefaultCamelContext;
 *	
 *	public class CdiCamelContextFactory {
 *	
 *	    @Produces
 *	    @ApplicationScoped
 *	    @Named("camelContext")
 *	    CamelContext createCamelContext() throws Exception {
 *	        CamelContext camelContext = new DefaultCamelContext();
 *	        return camelContext;
 *	    }
 *	}
 *  }
 * </pre>
 * 
 * 
 * @author Zach Visagie
 *
 */
public abstract class CdiCamelBehavior extends CamelBehavior {
    private static final long serialVersionUID = 1L;

    @Override
    protected void setAppropriateCamelContext(DelegateExecution execution) {
        String camelContextValue = getStringFromField(camelContext, execution);
        if (StringUtils.isEmpty(camelContextValue) && camelContextObj != null) {
            // already set no further processing needed
        } else {
            ProcessEngineConfiguration engineConfiguration = org.flowable.engine.impl.context.Context.getProcessEngineConfiguration();
            if (StringUtils.isEmpty(camelContextValue) && camelContextObj == null) {
                camelContextValue = engineConfiguration.getDefaultCamelContext();
            }
        	
            camelContextObj = get(camelContextValue);
            
        	if(camelContextObj == null && camelContextValue.equals(engineConfiguration.getDefaultCamelContext())) {
        		// Camel CDI's default context is not named, so lookup as class if no named ones could be found
        		// See org.flowable.camel.cdi.named.CdiContextFactory to create a named context 
            	// lookup default context in CDI container so that 
            	camelContextObj = ProgrammaticBeanLookup.lookup(CamelContext.class);
            }
        }
    }

    protected CamelContext get(String name) {
        BeanManager beanManager = BeanManagerLookup.getBeanManager();
        Set<Bean<?>> beans = beanManager.getBeans(name);
        if(beans.isEmpty())
        	return null;
		Bean<CamelContext> bean = (Bean<CamelContext>) beans.iterator().next();
        CreationalContext<CamelContext> ctx = beanManager.createCreationalContext(bean);
        return (CamelContext) beanManager.getReference(bean, CamelContext.class, ctx);
    }

}
