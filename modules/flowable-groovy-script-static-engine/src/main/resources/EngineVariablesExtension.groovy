//package org.flowable.engine

/**
 * Helper script for handling dynamic process variables
 * @author Filip Grochowski
 * Created by fgroch on 09.03.17.
 */
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassCodeVisitorSupport
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.sc.StaticCompilationMetadataKeys
import org.codehaus.groovy.transform.stc.ExtensionMethodNode
import org.codehaus.groovy.transform.stc.GroovyTypeCheckingExtensionSupport
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport

import static org.flowable.engine.impl.scripting.GroovyStaticScriptEngine.*
import org.flowable.engine.delegate.VariableScope

def typesOfVariables = COMPILE_OPTIONS.get()[VAR_TYPES]

unresolvedVariable { var ->
    if (typesOfVariables[var.name]) {
        storeType(var, typesOfVariables[var.name])
        return makeDynamic(var, typesOfVariables[var.name])
    }
}

/*class EngineVariabeExtension extends GroovyTypeCheckingExtensionSupport.TypeCheckingDSL {

    @CompileStatic
    private static String prettyPrint(ClassNode node) {
        node.isArray()?"${prettyPrint(node.componentType)}[]":node.toString(false)
    }

    @CompileStatic
    private static String toMethodDescriptor(MethodNode node) {
        if (node instanceof ExtensionMethodNode) {
            return toMethodDescriptor(node.getExtensionMethodNode())
        }
        def sb = new StringBuilder()
        sb.append(node.declaringClass.toString(false))
        sb.append("#")
        sb.append(node.name)
        sb.append('(')
        sb.append(node.parameters.collect { Parameter it ->
            prettyPrint(it.originType)
        }.join(','))
        sb.append(')')
        sb
    }

    @Override
    Object run() {

        // Fetch white list of regular expressions of authorized method calls
        //def whiteList = COMPILE_OPTIONS.get()[WHITELIST_PATTERNS]
        def typesOfVariables = COMPILE_OPTIONS.get()[VAR_TYPES]


        unresolvedVariable { var ->
            if (isDynamic(var) && typesOfVariables[var.name]) {
                storeType(var, typesOfVariables[var.name])
                handled = true
            }
        }
    }
}*/
