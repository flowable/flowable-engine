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
package org.flowable.dmn.spring.configurator.test;

import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.engine.impl.test.AbstractFlowableTestCase;
import org.flowable.spring.impl.test.InternalFlowableSpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Joram Barrez
 * @author Josh Long
 */
@EnsureCleanDb(excludeTables = {
    "ACT_GE_PROPERTY",
    "ACT_ID_PROPERTY",
    "ACT_DMN_DATABASECHANGELOGLOCK",
    "ACT_DMN_DATABASECHANGELOG",
    "FLW_EV_DATABASECHANGELOGLOCK",
    "FLW_EV_DATABASECHANGELOG"
})
@ExtendWith(InternalFlowableSpringExtension.class)
@ExtendWith(SpringExtension.class)
public abstract class SpringDmnFlowableTestCase extends AbstractFlowableTestCase implements ApplicationContextAware {

    @Autowired
    protected ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
