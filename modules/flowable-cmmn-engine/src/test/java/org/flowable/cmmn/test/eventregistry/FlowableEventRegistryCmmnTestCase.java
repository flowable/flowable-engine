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
package org.flowable.cmmn.test.eventregistry;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.junit.After;
import org.junit.Before;

/**
 * @author Filip Hrisafov
 */
public abstract class FlowableEventRegistryCmmnTestCase extends FlowableCmmnTestCase {

    protected Map<Object, Object> initialBeans;

    @Before
    public void setUpBeans() {
        ExpressionManager eventRegistryExpressionManager = getEventRegistryEngineConfiguration().getExpressionManager();
        initialBeans = eventRegistryExpressionManager.getBeans();
        eventRegistryExpressionManager.setBeans(new HashMap<>());
    }

    @After
    public void resetBeans() {
        getEventRegistryEngineConfiguration().getExpressionManager().setBeans(initialBeans);
    }
}
