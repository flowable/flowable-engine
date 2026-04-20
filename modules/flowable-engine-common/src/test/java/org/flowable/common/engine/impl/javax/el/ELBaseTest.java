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

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.impl.el.BaseElTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Base class for tests that (indirectly) use BeanSupport and want to test both implementations.
 */
@ParameterizedClass(name = "[{index}] useStandalone={0}")
@ValueSource(booleans = { true, false })
public abstract class ELBaseTest extends BaseElTest {

    @Parameter(0)
    public boolean useStandalone;

    @BeforeEach
    public void setupBase() {
        // Disable caching so we can switch implementations within a JVM instance.
        System.setProperty("jakarta.el.BeanSupport.doNotCacheInstance", "true");
        // Set up the implementation for this test run
        System.setProperty("jakarta.el.BeanSupport.useStandalone", Boolean.toString(useStandalone));
        BeanSupport.beanSupport = null;
    }

    /*
     * Double check test has been configured as expected
     */
    @Test
    public void testImplementation() {
        if (useStandalone) {
            assertThat(BeanSupport.getInstance()).isInstanceOf(BeanSupportStandalone.class);
        } else {
            assertThat(BeanSupport.getInstance()).isInstanceOf(BeanSupportFull.class);
        }
    }
}
