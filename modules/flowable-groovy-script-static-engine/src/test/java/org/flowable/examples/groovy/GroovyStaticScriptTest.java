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

package org.flowable.examples.groovy;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * @author Filip Grochowski
 */
public class GroovyStaticScriptTest extends PluggableFlowableTestCase {

    @Deployment
    public void testGroovyStaticScriptEngine() {
        int[] inputArray = new int[] { 1, 2, 3, 4, 5 };
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("staticScriptEngine", CollectionUtil.singletonMap("inputArray", inputArray));

        String result = (String) runtimeService.getVariable(pi.getId(), "a");
        Integer sum = (Integer) runtimeService.getVariable(pi.getId(), "sum");
        assertEquals("ABC", result);
        assertEquals(15, sum.intValue());
    }
    
    @Deployment
    public void testGroovyScriptEngine() {
        int[] inputArray = new int[] { 1, 2, 3, 4, 5 };
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("groovyScriptEngine", CollectionUtil.singletonMap("inputArray", inputArray));

        String result = (String) runtimeService.getVariable(pi.getId(), "a");
        Integer sum = (Integer) runtimeService.getVariable(pi.getId(), "sum");
        assertEquals("ABC", result);
        assertEquals(15, sum.intValue());
    }
}
