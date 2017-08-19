package org.flowable.camel;

import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.spring.SpringProcessEngineConfiguration;

public abstract class SpringCamelBehavior extends CamelBehavior {
    
    protected void setAppropriateCamelContext(DelegateExecution execution) {
        // Get the appropriate String representation of the CamelContext object
        // from ActivityExecution (if available).
        String camelContextValue = getStringFromField(camelContext, execution);

        // If the String representation of the CamelContext object from ActivityExecution is empty, use the default.
        if (StringUtils.isEmpty(camelContextValue) && camelContextObj != null) {
            // No processing required. No custom CamelContext & the default is already set.

        } else {
            // Get the ProcessEngineConfiguration object.
            ProcessEngineConfiguration engineConfiguration = org.flowable.engine.impl.context.Context.getProcessEngineConfiguration();
            if ((Context.getCommandContext() != null && Flowable5Util.isFlowable5ProcessDefinitionId(Context.getCommandContext(), execution.getProcessDefinitionId())) ||
                    (Context.getCommandContext() == null && Flowable5Util.getFlowable5CompatibilityHandler() != null)) {

                Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
                camelContextObj = (CamelContext) compatibilityHandler.getCamelContextObject(camelContextValue);

            } else {
                // Convert it to a SpringProcessEngineConfiguration. If this doesn't work, throw a RuntimeException.
                try {
                    SpringProcessEngineConfiguration springConfiguration = (SpringProcessEngineConfiguration) engineConfiguration;
                    if (StringUtils.isEmpty(camelContextValue) && camelContextObj == null) {
                        camelContextValue = springConfiguration.getDefaultCamelContext();
                    }

                    // Get the CamelContext object and set the super's member variable.
                    Object ctx = springConfiguration.getApplicationContext().getBean(camelContextValue);
                    if (!(ctx instanceof CamelContext)) {
                        throw new FlowableException("Could not find CamelContext named " + camelContextValue + ".");
                    }
                    camelContextObj = (CamelContext) ctx;

                } catch (Exception e) {
                    throw new FlowableException("Expecting a SpringProcessEngineConfiguration for the Camel module.", e);
                }
            }
        }
    }

    
}
