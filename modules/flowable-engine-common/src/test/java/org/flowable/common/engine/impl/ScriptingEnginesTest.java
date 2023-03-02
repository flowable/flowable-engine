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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.flowable.common.engine.impl.scripting.CompositeScriptTraceListener;
import org.flowable.common.engine.impl.scripting.FlowableScriptEvaluationException;
import org.flowable.common.engine.impl.scripting.MapResolver;
import org.flowable.common.engine.impl.scripting.ResolverFactory;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.common.engine.impl.scripting.ScriptEngineRequest;
import org.flowable.common.engine.impl.scripting.ScriptTrace;
import org.flowable.common.engine.impl.scripting.ScriptTraceListener;
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
                .script(script)
                .language("JavaScript")
                .variableContainer(VariableScope.empty())
                .build();
        // WHEN
        Object scriptResult = engines.evaluate(request).getResult();

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
                .script(script)
                .language("JavaScript")
                .variableContainer(VariableScope.empty())
                .additionalResolver(resolver)
                .build();

        // WHEN
        Object scriptResult = engines.evaluate(request).getResult();

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
                .script(script)
                .language("JavaScript")
                .variableContainer(VariableScope.empty())
                .additionalResolver(resolver)
                .build();

        // WHEN
        Object scriptResult = engines.evaluate(request).getResult();

        // THEN
        assertThat(scriptResult).isInstanceOfSatisfying(MyBean.class, result -> {
            assertThat(result).isSameAs(myBeanAdditionalResolver);
            assertThat(result).isNotSameAs(myBeanResolverFactory);
            assertThat(result.getBar()).isEqualTo("setInScript");
        });
        assertThat(myBeanResolverFactory.getBar()).isNull();
    }

    @Test
    public void expectScriptTraceEnhancersAreAppliedInCaseOfException() {
        String script = "throw Error('MyError')";
        engines.setDefaultTraceEnhancer(trace -> trace.addTraceTag("global", "foo"));

        ScriptEngineRequest request = ScriptEngineRequest.builder()
                .script(script)
                .language("JavaScript")
                .traceEnhancer(trace -> trace.addTraceTag("requestSpecific", "bar"))
                .build();
        assertThatThrownBy(() -> engines.evaluate(request))
                .isInstanceOfSatisfying(FlowableScriptEvaluationException.class, ex -> {
                    ScriptTrace errorTrace = ex.getErrorTrace();
                    assertThat(errorTrace.getTraceTags()).containsExactly(entry("global", "foo"), entry("requestSpecific", "bar"));
                    assertThat(ex.getMessage()).contains("MyError", "global=foo", "requestSpecific=bar");
                });
    }

    @Test
    public void expectScriptTraceEnhancersAreAppliedForErrorTraceListenerAndCalledForErrorsOnlyByDefault() {
        // GIVEN
        engines.setDefaultTraceEnhancer(trace -> trace.addTraceTag("global", "foo"));
        List<ScriptTrace> capturedTrace = new LinkedList<>();
        engines.setScriptErrorListener(trace -> capturedTrace.add(trace));

        // WHEN
        ScriptEngineRequest errorRequest = ScriptEngineRequest.builder()
                .script("throw Error('MyError');")
                .language("JavaScript")
                .traceEnhancer(trace -> trace.addTraceTag("requestSpecific", "bar"))
                .build();
        assertThatThrownBy(() -> engines.evaluate(errorRequest)).isInstanceOf(FlowableScriptEvaluationException.class);

        ScriptEngineRequest successRequest = ScriptEngineRequest.builder()
                .script("var foo = 'bar';")
                .language("JavaScript")
                .traceEnhancer(trace -> trace.addTraceTag("requestSpecific", "bar"))
                .build();
        engines.evaluate(successRequest);

        // THEN
        assertThat(capturedTrace).describedAs("expected only error request to have been captured")
                .singleElement().satisfies(c -> {
                    assertThat(c.getTraceTags()).containsExactly(entry("global", "foo"), entry("requestSpecific", "bar"));
                    assertThat(c.getException()).isNotNull();
                    assertThat(c.hasException()).isTrue();
                });
    }

    @Test
    public void expectScriptTraceEnhancersAreAppliedForErrorTraceListenerAndCapturesAll() {
        // GIVEN
        engines.setDefaultTraceEnhancer(trace -> trace.addTraceTag("global", "foo"));
        List<ScriptTrace> capturedTrace = new LinkedList<>();
        ScriptTraceListener listener = scriptTrace -> capturedTrace.add(scriptTrace);
        engines.setScriptErrorListener(listener);
        engines.setScriptSuccessListener(listener);

        // WHEN
        ScriptEngineRequest errorRequest = ScriptEngineRequest.builder()
                .script("throw Error('MyError');")
                .language("JavaScript")
                .traceEnhancer(trace -> trace.addTraceTag("requestSpecific", "bar"))
                .build();
        assertThatThrownBy(() -> engines.evaluate(errorRequest)).isInstanceOf(FlowableScriptEvaluationException.class);

        ScriptEngineRequest successRequest = ScriptEngineRequest.builder()
                .script("var foo = 'bar';")
                .language("JavaScript")
                // Note no additional enhancer in request
                .build();
        engines.evaluate(successRequest);

        // THEN
        assertThat(capturedTrace).hasSize(2);
        assertThat(capturedTrace.get(0)).satisfies(c -> {
            assertThat(c.getTraceTags()).containsExactly(entry("global", "foo"), entry("requestSpecific", "bar"));
            assertThat(c.getException()).isNotNull();
            assertThat(c.hasException()).isTrue();
        });

        assertThat(capturedTrace.get(1)).satisfies(c -> {
            assertThat(c.getTraceTags()).containsExactly(entry("global", "foo"));
            assertThat(c.getException()).isNull();
            assertThat(c.hasException()).isFalse();
        });
    }

    @Test
    public void epxectCompoundTraceListenerAppendsAll() {
        // GIVEN
        engines.setDefaultTraceEnhancer(trace -> trace.addTraceTag("global", "foo"));
        List<ScriptTrace> capturedTrace = new LinkedList<>();
        ScriptTraceListener listener = scriptTrace -> capturedTrace.add(scriptTrace);
        engines.setScriptErrorListener(new CompositeScriptTraceListener(Arrays.asList(listener, listener)));
        engines.setScriptSuccessListener(new CompositeScriptTraceListener(Arrays.asList(listener, listener)));

        // WHEN
        ScriptEngineRequest errorRequest = ScriptEngineRequest.builder()
                .script("throw Error('MyError');")
                .language("JavaScript")
                .build();
        assertThatThrownBy(() -> engines.evaluate(errorRequest)).isInstanceOf(FlowableScriptEvaluationException.class);

        ScriptEngineRequest successRequest = ScriptEngineRequest.builder()
                .script("var foo = 'bar';")
                .language("JavaScript")
                // Note no additional enhancer in request
                .build();
        engines.evaluate(successRequest);

        // THEN
        assertThat(capturedTrace).hasSize(4);
        assertThat(capturedTrace.get(0)).satisfies(c -> {
            assertThat(c.getException()).isNotNull();
            assertThat(c.hasException()).isTrue();
        });

        assertThat(capturedTrace.get(2)).satisfies(c -> {
            assertThat(c.getException()).isNull();
            assertThat(c.hasException()).isFalse();
        });
    }

    public static class MyBean {

        protected String foo;
        protected String bar;

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
