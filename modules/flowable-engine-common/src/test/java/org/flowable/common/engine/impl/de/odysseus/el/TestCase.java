/*
 * Copyright 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.de.odysseus.el;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

import org.flowable.common.engine.impl.de.odysseus.el.tree.Tree;
import org.flowable.common.engine.impl.de.odysseus.el.tree.TreeStore;
import org.flowable.common.engine.impl.de.odysseus.el.tree.impl.Builder;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.flowable.common.engine.impl.javax.el.ExpressionFactory;
import org.flowable.common.engine.impl.javax.el.ValueExpression;

public abstract class TestCase {

    protected static final Builder BUILDER = new Builder(Builder.Feature.METHOD_INVOCATIONS);

    protected ExpressionFactory expressionFactory;

    protected static Tree parse(String expression) {
        return BUILDER.build(expression);
    }

    protected Object eval(String expression) {
        return eval(expression, Object.class);
    }
    protected Object eval(String expression, ELContext context) {
        return eval(expression, context, Object.class);
    }

    protected <T> T eval(String expression, Class<T> expectedType) {
        return eval(expression, null, expectedType);
    }
    protected <T> T eval(String expression, ELContext context, Class<T> expectedType) {
        if (context == null) {
            context = new SimpleContext();
        }
        ValueExpression valueExpression = getExpressionFactory().createValueExpression(context, expression, expectedType);
        return (T) valueExpression.getValue(context);
    }

    protected ExpressionFactory getExpressionFactory() {
        if (expressionFactory == null) {
            expressionFactory = createExpressionFactory();
        }
        return expressionFactory;
    }

    protected ExpressionFactory createExpressionFactory() {
        TreeStore store = new TreeStore(BUILDER, null);
        return new ExpressionFactoryImpl(store);
    }

    protected static byte[] serialize(Object value) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bout);
        out.writeObject(value);
        out.close();
        return bout.toByteArray();
    }

    protected static Object deserialize(byte[] bytes) throws Exception {
        ByteArrayInputStream bin = new ByteArrayInputStream(bytes);
        ObjectInput in = new ObjectInputStream(bin);
        return in.readObject();
    }
}
