/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.javax.el;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class LambdaExpression {

    private final List<String> formalParameters;
    private final ValueExpression expression;
    private final Map<String, Object> nestedArguments = new HashMap<>();
    private ELContext context = null;

    public LambdaExpression(List<String> formalParameters, ValueExpression expression) {
        this.formalParameters = formalParameters;
        this.expression = expression;

    }

    public void setELContext(ELContext context) {
        this.context = context;
    }

    @SuppressWarnings("null") // args[i] can't be null due to earlier checks
    public Object invoke(ELContext context, Object... args) throws ELException {

        Objects.requireNonNull(context, "context is null");

        int formalParamCount = 0;
        if (formalParameters != null) {
            formalParamCount = formalParameters.size();
        }

        int argCount = 0;
        if (args != null) {
            argCount = args.length;
        }

        if (formalParamCount > argCount) {
            throw new ELException("Only '" + argCount + "' arguments were provided for a lambda expression that requires at least '" + formalParamCount + "''");
        }

        // Build the argument map
        // Start with the arguments from any outer expressions so if there is
        // any overlap the local arguments have priority
        Map<String, Object> lambdaArguments = new HashMap<>(nestedArguments);
        for (int i = 0; i < formalParamCount; i++) {
            lambdaArguments.put(formalParameters.get(i), args[i]);
        }

        context.enterLambdaScope(lambdaArguments);

        try {
            Object result = expression.getValue(context);
            // Make arguments from this expression available to any nested
            // expression
            if (result instanceof LambdaExpression) {
                ((LambdaExpression) result).nestedArguments.putAll(lambdaArguments);
            }
            return result;
        } finally {
            context.exitLambdaScope();
        }
    }

    public Object invoke(Object... args) {
        return invoke(context, args);
    }

    public List<String> getFormalParameters() {
        return formalParameters;
    }
}
