package org.flowable.camel;

import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.flowable.cdi.impl.util.ProgrammaticBeanLookup;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;

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
            camelContextObj = (CamelContext) ProgrammaticBeanLookup.lookup(camelContextValue);
        }
    }

}
