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
package org.flowable.common.engine.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.scripting.MapResolver;
import org.flowable.common.engine.impl.scripting.ResolverFactory;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.common.engine.impl.scripting.ScriptEngineRequest;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.variable.api.delegate.VariableScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ScriptingEnginesTest {

    List<ResolverFactory> resolverFactories;
    ScriptingEngines engines;

    @BeforeEach
    public void init() {
        resolverFactories = new ArrayList<>();
        ScriptBindingsFactory factory = new ScriptBindingsFactory(null, resolverFactories);

        engines = new ScriptingEngines(factory);
    }

    @AfterEach
    public void cleanup() {
        resolverFactories.clear();
    }

    @Test
    public void expectVariableResolvedFromResolverFactory() {
        // GIVEN
        MapResolver factoryMapResolver = new MapResolver().put("myBean", new MyBean());
        ResolverFactory resolverFactory = (config, variableScope) -> factoryMapResolver;
        resolverFactories.add(resolverFactory);
        String script = "myBean.setBar('setInScript'); myBean";


        ScriptEngineRequest request = ScriptEngineRequest.builder()
                .setScript(script)
                .setLanguage("JavaScript")
                .setVariableContainer(VariableScope.empty())
                .build();
        // WHEN
        Object scriptResult = engines.evaluate(request);

        // THEN
        assertThat(scriptResult).isInstanceOfSatisfying(MyBean.class, result -> {
            assertThat(result.getBar()).isEqualTo("setInScript");
        });
    }

    @Test
    public void expectVariableResolvedFromAdditionalResolver() {
        // GIVEN
        MapResolver resolver = new MapResolver().put("myBean", new MyBean());
        String script = "myBean.setBar('setInScript'); myBean";

        ScriptEngineRequest request = ScriptEngineRequest.builder()
                .setScript(script)
                .setLanguage("JavaScript")
                .setVariableContainer(VariableScope.empty())
                .addAdditionalResolver(resolver)
                .build();

        // WHEN
        Object scriptResult = engines.evaluateWithEvaluationResult(request).getResult();

        // THEN
        assertThat(scriptResult).isInstanceOfSatisfying(MyBean.class, result -> {
            assertThat(result.getBar()).isEqualTo("setInScript");
        });
    }

    @Test
    public void expectVariableResolvedFromAdditionalResolverTakesPrecedence() {
        // GIVEN
        MyBean myBeanResolverFactory = new MyBean();

        MapResolver factoryMapResolver = new MapResolver().put("myBean", new MyBean());
        ResolverFactory resolverFactory = (config, variableScope) -> factoryMapResolver;
        resolverFactories.add(resolverFactory);

        MyBean myBeanAdditionalResolver = new MyBean();
        MapResolver resolver = new MapResolver().put("myBean", myBeanAdditionalResolver);

        String script = "myBean.setBar('setInScript'); myBean";

        ScriptEngineRequest request = ScriptEngineRequest.builder()
                .setScript(script)
                .setLanguage("JavaScript")
                .setVariableContainer(VariableScope.empty())
                .addAdditionalResolver(resolver)
                .build();

        // WHEN
        Object scriptResult = engines.evaluateWithEvaluationResult(request).getResult();

        // THEN
        assertThat(scriptResult).isInstanceOfSatisfying(MyBean.class, result -> {
            assertThat(result).isSameAs(myBeanAdditionalResolver);
            assertThat(result).isNotSameAs(myBeanResolverFactory);
            assertThat(result.getBar()).isEqualTo("setInScript");
        });
        assertThat(myBeanResolverFactory.getBar()).isNull();
    }

   public static class MyBean {
        protected String foo;
        protected  String bar;

        public String getBar() {
            return bar;
        }

        public void setBar(String bar) {
            this.bar = bar;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }
}
