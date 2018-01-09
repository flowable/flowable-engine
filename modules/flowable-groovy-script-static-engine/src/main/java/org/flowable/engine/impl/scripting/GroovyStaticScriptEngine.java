package org.flowable.engine.impl.scripting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import groovy.transform.CompileStatic;

/**
 * Created by fgroch on 15.03.17.
 */
public class GroovyStaticScriptEngine extends GroovyScriptEngineImpl {

    public static final String VAR_TYPES = "flowable.variable.types";

    public static final ThreadLocal<Map<String, Object>> COMPILE_OPTIONS = new ThreadLocal<>();

    // lazily initialized factory
    private volatile GroovyStaticScriptEngineFactory factory;

    private static Class<?> clazz;

    public GroovyStaticScriptEngine(GroovyStaticScriptEngineFactory factory) {
        this();
        this.factory = factory;
    }

    public GroovyStaticScriptEngine() {
        this(new GroovyClassLoader(getParentLoader(), createStaticConfiguration(), true));
    }

    public GroovyStaticScriptEngine(GroovyClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    @Override
    public Object eval(String script, ScriptContext ctx) throws ScriptException {
        COMPILE_OPTIONS.remove();
        Map<String, ClassNode> variableTypes = new HashMap<>();
        for (Map.Entry<String, Object> entry : ctx.getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
            variableTypes.put(entry.getKey(),
                    ClassHelper.make(entry.getValue().getClass()));
        }

        variableTypes.put("execution", ClassHelper.make(clazz));
        Map<String, Object> options = new HashMap<>();
        options.put(VAR_TYPES, variableTypes);
        COMPILE_OPTIONS.set(options);
        Object ret = super.eval(script, ctx);
        return ret;
    }

    protected static CompilerConfiguration createStaticConfiguration() {
        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        ASTTransformationCustomizer astTransformationCustomizer = new ASTTransformationCustomizer(
                Collections.singletonMap("extensions", Collections.singletonList("EngineVariablesExtension.groovy")),
                CompileStatic.class, "org.codehaus.groovy.transform.sc.StaticCompileTransformation");
        compilerConfiguration.addCompilationCustomizers(astTransformationCustomizer);
        return compilerConfiguration;
    }

    private static ClassLoader getParentLoader() {
        ClassLoader ctxtLoader = Thread.currentThread().getContextClassLoader();
        try {
            Class<?> scriptClass = ctxtLoader.loadClass(Script.class.getName());
            clazz = ctxtLoader.loadClass("org.flowable.variable.api.delegate.VariableScope");

            if (scriptClass == Script.class) {
                return ctxtLoader;
            }
        } catch (ClassNotFoundException var2) {
            var2.printStackTrace();
        }
        return Script.class.getClassLoader();
    }
}
