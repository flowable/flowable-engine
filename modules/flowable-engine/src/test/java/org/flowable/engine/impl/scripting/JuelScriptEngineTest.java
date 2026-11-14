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

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

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

}
