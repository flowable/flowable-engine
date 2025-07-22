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
package org.flowable.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.spring.SpringProcessEngineConfiguration;

/**
 * Camel behavior that looks up the camel context when using SpringProcessEngineConfiguration.
 * 
 * @author Zach Visagie
 */
public abstract class SpringCamelBehavior extends CamelBehavior {
    
	private static final long serialVersionUID = 1L;

    protected final Object contextLock = new Object();

    @Override
    protected CamelContext getCamelContext(DelegateExecution execution, boolean isV5Execution) {
        // Get the appropriate String representation of the CamelContext object
        // from ActivityExecution (if available).
        String camelContextValue = getStringFromField(camelContext, execution);
        if (StringUtils.isEmpty(camelContextValue)) {
            if (camelContextObj != null) {
                // No processing required. No custom CamelContext & the default is already set.
                return camelContextObj;
            }

            // Use a lock to resolve the default camel context.
            synchronized (contextLock) {
                // Check again, in case another thread set the member variable in the meantime.
                if (camelContextObj != null) {
                    return camelContextObj;
                }
                camelContextObj = resolveCamelContext(camelContextValue, isV5Execution);
            }

            return camelContextObj;
        } else {
            return resolveCamelContext(camelContextValue, isV5Execution);
        }
    }

    protected CamelContext resolveCamelContext(String camelContextValue, boolean isV5Execution) {
        ProcessEngineConfiguration engineConfiguration = org.flowable.engine.impl.context.Context.getProcessEngineConfiguration();
        if (isV5Execution) {

            Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler();
            return (CamelContext) compatibilityHandler.getCamelContextObject(camelContextValue);

        } else {
            // Convert it to a SpringProcessEngineConfiguration. If this doesn't work, throw a RuntimeException.
            try {
                SpringProcessEngineConfiguration springConfiguration = (SpringProcessEngineConfiguration) engineConfiguration;
                if (StringUtils.isEmpty(camelContextValue)) {
                    camelContextValue = springConfiguration.getDefaultCamelContext();
                }

                // Get the CamelContext object and set the super's member variable.
                Object ctx = springConfiguration.getApplicationContext().getBean(camelContextValue);
                if (!(ctx instanceof SpringCamelContext)) {
                    throw new FlowableException("Could not find CamelContext named " + camelContextValue + ".");
                }
                return (SpringCamelContext) ctx;

            } catch (Exception e) {
                throw new FlowableException("Expecting a SpringProcessEngineConfiguration for the Camel module.", e);
            }
        }
    }

    
}
