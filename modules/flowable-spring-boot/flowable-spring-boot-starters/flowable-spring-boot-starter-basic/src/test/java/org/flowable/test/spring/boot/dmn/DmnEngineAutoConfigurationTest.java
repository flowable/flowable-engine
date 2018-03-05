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
package org.flowable.test.spring.boot.dmn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.dmn.api.DmnEngineConfigurationApi;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.spring.boot.FlowableTransactionAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

public class DmnEngineAutoConfigurationTest {

    @Test
    public void standaloneDmnEngineWithBasicDataSource() {
        AnnotationConfigApplicationContext context = this.context(DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
                        DmnEngineServicesAutoConfiguration.class, DmnEngineAutoConfiguration.class);

        DmnEngine dmnEngine = context.getBean(DmnEngine.class);
        assertThat(dmnEngine).as("Dmn engine").isNotNull();

        assertAllServicesPresent(context, dmnEngine);
    }

    private void assertAllServicesPresent(AnnotationConfigApplicationContext context, DmnEngine dmnEngine) {
        List<Method> methods = Stream.of(DmnEngine.class.getDeclaredMethods())
                        .filter(method -> !(method.getName().equals("close") || method.getName().equals("getName"))).collect(Collectors.toList());

        assertThat(methods).allSatisfy(method -> {
            try {
                assertThat(context.getBean(method.getReturnType())).as(method.getReturnType() + " bean").isEqualTo(method.invoke(dmnEngine));
            } catch (IllegalAccessException | InvocationTargetException e) {
                fail("Failed to invoke method " + method, e);
            }
        });
    }

    @Test
    public void dmnEngineWithBasicDataSourceAndProcessEngine() {
        AnnotationConfigApplicationContext context = this
                        .context(DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, DmnEngineAutoConfiguration.class,
                                        HibernateJpaAutoConfiguration.class, FlowableTransactionAutoConfiguration.class, ProcessEngineAutoConfiguration.class,
                                        DmnEngineServicesAutoConfiguration.class);

        ProcessEngine processEngine = context.getBean(ProcessEngine.class);
        assertThat(processEngine).as("Process engine").isNotNull();
        DmnEngineConfigurationApi dmnProcessConfigurationApi = dmnEngine(processEngine);

        DmnEngine dmnEngine = context.getBean(DmnEngine.class);
        assertThat(dmnEngine.getDmnEngineConfiguration()).as("Dmn Engine Configuration").isEqualTo(dmnProcessConfigurationApi);
        assertThat(dmnEngine).as("Dmn engine").isNotNull();

        assertAllServicesPresent(context, dmnEngine);
    }

    private AnnotationConfigApplicationContext context(Class<?>... clazz) {
        AnnotationConfigApplicationContext annotationConfigApplicationContext = new AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(clazz);
        Map<String, Object> values = new HashMap<>();
        values.put("flowable.dmn.deployResources", false);
        annotationConfigApplicationContext.getEnvironment().getPropertySources().addFirst(new MapPropertySource("831-override", values));
        annotationConfigApplicationContext.refresh();
        return annotationConfigApplicationContext;
    }

    private static DmnEngineConfigurationApi dmnEngine(ProcessEngine processEngine) {
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        return EngineServiceUtil.getDmnEngineConfiguration(processEngineConfiguration);
    }
}
