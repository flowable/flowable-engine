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

package org.flowable.dmn.engine.impl.test;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.dmn.engine.impl.DmnEngineImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for the flowable DMN test cases.
 *
 * @author Tijs Rademakers
 */
public abstract class PluggableFlowableDmnTestCase extends AbstractFlowableDmnTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluggableFlowableDmnTestCase.class);

    protected static DmnEngine cachedDmnEngine;

    @Override
    protected void initializeDmnEngine() {
        if (cachedDmnEngine == null) {
            LOGGER.info("No cached dmn engine found for test. Retrieving the default engine.");
            DmnEngines.destroy(); // Just to be sure we're not getting any previously cached version

            cachedDmnEngine = DmnEngines.getDefaultDmnEngine();
            if (cachedDmnEngine == null) {
                throw new FlowableException("no default dmn engine available");
            }
        }

        dmnEngine = cachedDmnEngine;
        dmnEngineConfiguration = ((DmnEngineImpl) dmnEngine).getDmnEngineConfiguration();
    }

}
