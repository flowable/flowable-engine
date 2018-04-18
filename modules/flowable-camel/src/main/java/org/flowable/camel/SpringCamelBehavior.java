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
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
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
