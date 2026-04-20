/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.el.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleContext;
import org.flowable.common.engine.impl.de.odysseus.el.util.SimpleResolver;
import org.flowable.common.engine.impl.el.BaseElTest;
import org.flowable.common.engine.impl.el.ReadOnlyMapELResolver;
import org.flowable.common.engine.impl.javax.el.CompositeELResolver;
import org.flowable.common.engine.impl.javax.el.ELContext;
import org.junit.jupiter.api.Test;

class AstValueTest extends BaseElTest {

    @Test
    void testNull01() {
        TesterBeanB beanB = new TesterBeanB();
        assertThat(beanB.getText()).isNull();
        CompositeELResolver resolver = new CompositeELResolver(List.of(
                new ReadOnlyMapELResolver(Map.of(
                        "bean", beanB
                )),
                new SimpleResolver()
        ));
        ELContext context = new SimpleContext(resolver);
        Object result = eval("bean.text.toLowerCase()", context);
        assertThat(result).isNull();
    }
}
