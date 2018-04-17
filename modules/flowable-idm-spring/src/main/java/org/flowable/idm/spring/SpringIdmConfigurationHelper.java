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

package org.flowable.idm.spring;

import java.net.URL;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.idm.engine.IdmEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.UrlResource;

/**
 * @author Tom Baeyens
 */
public class SpringIdmConfigurationHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringIdmConfigurationHelper.class);

    public static IdmEngine buildIdmEngine(URL resource) {
        LOGGER.debug("==== BUILDING SPRING APPLICATION CONTEXT AND IDM ENGINE =========================================");

        ApplicationContext applicationContext = new GenericXmlApplicationContext(new UrlResource(resource));
        Map<String, IdmEngine> beansOfType = applicationContext.getBeansOfType(IdmEngine.class);
        if ((beansOfType == null) || (beansOfType.isEmpty())) {
            throw new FlowableException("no " + IdmEngine.class.getName() + " defined in the application context " + resource.toString());
        }

        IdmEngine idmEngine = beansOfType.values().iterator().next();

        LOGGER.debug("==== SPRING IDM ENGINE CREATED ==================================================================");
        return idmEngine;
    }

}
