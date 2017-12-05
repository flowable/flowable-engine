//package org.flowable.engine

/**
 * Helper script for handling dynamic process variables
 * @author Filip Grochowski
 * Created by fgroch on 09.03.17.
 */

import static org.flowable.engine.impl.scripting.GroovyStaticScriptEngine.*
import org.flowable.variable.api.delegate.VariableScope

def typesOfVariables = COMPILE_OPTIONS.get()[VAR_TYPES]

unresolvedVariable { var ->
    if (typesOfVariables[var.name]) {
        return makeDynamic(var, typesOfVariables[var.name])
    }
}
