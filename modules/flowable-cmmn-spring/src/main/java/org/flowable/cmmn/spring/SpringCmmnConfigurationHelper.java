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

package org.flowable.cmmn.spring;

import java.net.URL;
import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.common.engine.api.FlowableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.UrlResource;

/**
 * @author Tijs Rademakers
 */
public class SpringCmmnConfigurationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringCmmnConfigurationHelper.class);

    public static CmmnEngine buildCmmnEngine(URL resource) {
        LOGGER.debug("==== BUILDING SPRING APPLICATION CONTEXT AND CMMN ENGINE =========================================");

        try (GenericXmlApplicationContext applicationContext = new GenericXmlApplicationContext(new UrlResource(resource))) {
            Map<String, CmmnEngine> beansOfType = applicationContext.getBeansOfType(CmmnEngine.class);
            if ((beansOfType == null) || (beansOfType.isEmpty())) {
                throw new FlowableException("no " + CmmnEngine.class.getName() + " defined in the application context " + resource.toString());
            }

            CmmnEngine cmmnEngine = beansOfType.values().iterator().next();

            LOGGER.debug("==== SPRING CMMN ENGINE CREATED ==================================================================");
            return cmmnEngine;
        }
    }

}
