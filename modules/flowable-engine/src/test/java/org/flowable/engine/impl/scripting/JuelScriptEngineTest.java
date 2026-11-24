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
package org.flowable.engine.impl.scripting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.javax.el.BeanELResolver;
import org.flowable.common.engine.impl.javax.el.CompositeELResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ELException;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.javax.el.MapELResolver;
import org.flowable.common.engine.impl.scripting.FlowableScriptException;
import org.flowable.common.engine.impl.scripting.JSR223FlowableScriptEngine;
import org.flowable.common.engine.impl.scripting.Resolver;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class JuelScriptEngineTest {

    @Test
    void importFunctionIsDisabledByDefault() {
        ScriptEngine scriptEngine = new JuelScriptEngineFactory().getScriptEngine();
        ScriptContext scriptContext = new SimpleScriptContext();

        assertThatThrownBy(() -> scriptEngine.eval("${lang:import(context, 'math', 'java.lang.Math')}", scriptContext))
                .isInstanceOf(ScriptException.class)
                .hasMessageContaining("lang:import");
        assertThat(scriptContext.getAttribute("lang:import")).isNull();
    }

    @Test
    void importFunctionCanBeEnabled() throws ScriptException {
        JuelScriptEngineFactory scriptEngineFactory = new JuelScriptEngineFactory();
        scriptEngineFactory.setImportFunctionEnabled(true);
        ScriptEngine scriptEngine = scriptEngineFactory.getScriptEngine();
        ScriptContext scriptContext = new SimpleScriptContext();

        scriptEngine.eval("${lang:import(context, 'integer', 'java.lang.Integer')}", scriptContext);

        assertThat(scriptEngine.eval("${integer:toHexString(255)}", scriptContext)).isEqualTo("ff");
    }

    @Test
    void printWritesToScriptContextWriter() throws ScriptException {
        ScriptEngine scriptEngine = new JuelScriptEngineFactory().getScriptEngine();
        ScriptContext scriptContext = new SimpleScriptContext();
        StringWriter writer = new StringWriter();
        scriptContext.setWriter(writer);
        scriptContext.setAttribute("name", "Kermit", ScriptContext.ENGINE_SCOPE);

        scriptEngine.eval("${out:print('Hello ')}${out:print(name)}", scriptContext);

        assertThat(writer).hasToString("Hello Kermit");
    }

    @Test
    void customScriptEngineFactoryCanProvideDifferentElResolver() throws FlowableScriptException {
        JSR223FlowableScriptEngine scriptEngine = new JSR223FlowableScriptEngine();
        scriptEngine.addScriptEngineFactory(new NoMethodInvocationJuelScriptEngineFactory());
        Map<String, Object> variables = Collections.singletonMap("person", Collections.singletonMap("name", "Kermit"));

        assertThat(evaluate(scriptEngine, "${person.name}", variables)).isEqualTo("Kermit");
        assertThatThrownBy(() -> evaluate(scriptEngine, "${person.name.toUpperCase()}", variables))
                .isInstanceOf(FlowableScriptException.class)
                .hasMessageContaining("Method invocation is not allowed");

        assertThat(evaluate(new JSR223FlowableScriptEngine(), "${person.name.toUpperCase()}", variables)).isEqualTo("KERMIT");
    }

    protected Object evaluate(JSR223FlowableScriptEngine scriptEngine, String script, Map<String, Object> variables) throws FlowableScriptException {
        return scriptEngine.createEvaluationRequest()
                .language("juel")
                .script(script)
                .resolver(new MapResolver(variables))
                .scopeContainer(VariableContainer.empty())
                .inputVariableContainer(VariableContainer.empty())
                .evaluate()
                .getResult();
    }

    protected static class MapResolver implements Resolver {

        protected final Map<String, Object> variables;

        public MapResolver(Map<String, Object> variables) {
            this.variables = variables;
        }

        @Override
        public boolean containsKey(Object key) {
            return variables.containsKey(key);
        }

        @Override
        public Object get(Object key) {
            return variables.get(key);
        }
    }

    protected static class NoMethodInvocationJuelScriptEngineFactory extends JuelScriptEngineFactory {

        @Override
        public ScriptEngine getScriptEngine() {
            return new NoMethodInvocationJuelScriptEngine(this);
        }
    }

    protected static class NoMethodInvocationJuelScriptEngine extends JuelScriptEngine {

        public NoMethodInvocationJuelScriptEngine(JuelScriptEngineFactory scriptEngineFactory) {
            super(scriptEngineFactory, false);
        }

        @Override
        protected ELResolver createElResolver() {
            CompositeELResolver compositeResolver = new CompositeELResolver();
            compositeResolver.add(new MapELResolver());
            compositeResolver.add(new NoMethodInvocationBeanELResolver());
            return new SimpleResolver(compositeResolver);
        }
    }

    protected static class NoMethodInvocationBeanELResolver extends BeanELResolver {

        @Override
        public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
            throw new ELException("Method invocation is not allowed");
        }
    }

}
