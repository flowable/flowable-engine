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
package org.flowable.dmn.engine.test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.flowable.dmn.engine.CustomExpressionFunctionRegistry;
import org.flowable.dmn.engine.impl.mvel.extension.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class TestCustomExpressionFunctionRegistry implements CustomExpressionFunctionRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCustomExpressionFunctionRegistry.class);

    protected static Map<String, Method> customFunctionConfigurations = new HashMap<String, Method>();

    static {
        addCustomFunction("fn_testFunctionName", getMethod(DateUtil.class, "toDate", String.class));
    }

    @Override
    public Map<String, Method> getCustomExpressionMethods() {
        return customFunctionConfigurations;
    }

    protected static void addCustomFunction(String methodName, Method methodRef) {
        customFunctionConfigurations.put(methodName, methodRef);
    }

    protected static Method getMethod(Class classRef, String methodName, Class... methodParm) {
        try {
            LOGGER.debug("adding method to MVEL: {} {} with {} parameters", classRef.getName(), methodName, methodParm.length);
            return classRef.getMethod(methodName, methodParm);
        } catch (NoSuchMethodException nsme) {
            LOGGER.error("Could not find method for name: {}", methodName, nsme);
        }

        return null;
    }
}
