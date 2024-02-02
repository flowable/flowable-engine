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

import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;

/**
 * @author Filip Grochowski
 * Created by fgroch on 15.03.17.
 */
public class GroovyStaticScriptEngineFactory extends GroovyScriptEngineFactory {

    /**
     * Returns the full  name of the <code>ScriptEngine</code>. 
     *
     * @return The name of the engine implementation.
     */
    @Override
    public String getEngineName() {
        return "groovy-static";
    }

    @Override
    public List<String> getNames() {
        return Collections.singletonList("groovy-static");
    }

    /**
     * Returns the version of the <code>ScriptEngine</code>.
     *
     * @return The <code>ScriptEngine</code> implementation version.
     */
    @Override
    public String getEngineVersion() {
        return "1.0";
    }

    /**
     * Returns an instance of the <code>ScriptEngine</code> associated with this
     * <code>ScriptEngineFactory</code>. A new ScriptEngine is generally
     * returned, but implementations may pool, share or reuse engines.
     *
     * @return A new <code>ScriptEngine</code> instance.
     */
    @Override
    public ScriptEngine getScriptEngine() {
        return new GroovyStaticScriptEngine(this);
    }
}
