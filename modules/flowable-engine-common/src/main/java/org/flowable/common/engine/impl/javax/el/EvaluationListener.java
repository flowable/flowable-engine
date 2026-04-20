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

/**
 * @since EL 3.0
 */
public abstract class EvaluationListener {

    /**
     * Fired before the evaluation of the expression.
     *
     * @param context    The EL context in which the expression will be evaluated
     * @param expression The expression that will be evaluated
     */
    public void beforeEvaluation(ELContext context, String expression) {
        // NO-OP
    }

    /**
     * Fired after the evaluation of the expression.
     *
     * @param context    The EL context in which the expression was evaluated
     * @param expression The expression that was evaluated
     */
    public void afterEvaluation(ELContext context, String expression) {
        // NO-OP
    }

    /**
     * Fired after a property has been resolved.
     *
     * @param context  The EL context in which the property was resolved
     * @param base     The base object on which the property was resolved
     * @param property The property that was resolved
     */
    public void propertyResolved(ELContext context, Object base, Object property) {
        // NO-OP
    }
}
