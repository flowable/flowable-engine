package org.flowable.engine.impl.scripting;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;

import javax.script.ScriptEngine;
import java.util.Collections;
import java.util.List;

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
