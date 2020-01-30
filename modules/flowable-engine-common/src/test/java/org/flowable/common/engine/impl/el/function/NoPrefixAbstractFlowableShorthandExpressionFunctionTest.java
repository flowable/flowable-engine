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
package org.flowable.common.engine.impl.el.function;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class NoPrefixAbstractFlowableShorthandExpressionFunctionTest {

    // Fields are used in the TestNoPrefixFlowableShorthandExpressionFunction (nested non static class)
    // We can't use them in that class since those are fetched via the and getFinalFunctionPrefix getFunctionPrefixOptions
    // in the super constructor
    protected String finalFunctionPrefix;
    protected List<String> functionPrefixOptions;

    @Test
    void enhancingExpressionWithoutFinalFunctionPrefixShouldWork() {
        functionPrefixOptions = Arrays.asList("test", "assert");
        FlowableShortHandExpressionFunction shortHandExpressionFunction = new TestNoPrefixFlowableShorthandExpressionFunction();

        assertThat(shortHandExpressionFunction.enhance("test:empty('')"))
            .isEqualTo("isEmpty(scopeVariable,'')");

        assertThat(shortHandExpressionFunction.enhance("assert:empty('')"))
            .isEqualTo("isEmpty(scopeVariable,'')");

        assertThat(shortHandExpressionFunction.enhance("test:isEmpty('')"))
            .isEqualTo("isEmpty(scopeVariable,'')");

        assertThat(shortHandExpressionFunction.enhance("assert:isEmpty('')"))
            .isEqualTo("isEmpty(scopeVariable,'')");
    }

    @Test
    void enhancingExpressionWithoutFunctionPrefixOptionsShouldWork() {
        functionPrefixOptions = Collections.emptyList();
        FlowableShortHandExpressionFunction shortHandExpressionFunction = new TestNoPrefixFlowableShorthandExpressionFunction();

        assertThat(shortHandExpressionFunction.enhance("empty('')"))
            .isEqualTo("isEmpty(scopeVariable,'')");

        assertThat(shortHandExpressionFunction.enhance("isEmpty('')"))
            .isEqualTo("isEmpty(scopeVariable,'')");
    }

    @Test
    void enhancingExpressionWithoutFunctionPrefixOptionsWithFinalFunctionPrefixShouldWork() {
        finalFunctionPrefix = "test";
        functionPrefixOptions = Collections.emptyList();
        FlowableShortHandExpressionFunction shortHandExpressionFunction = new TestNoPrefixFlowableShorthandExpressionFunction();

        assertThat(shortHandExpressionFunction.enhance("empty('')"))
            .isEqualTo("test:isEmpty(scopeVariable,'')");

        assertThat(shortHandExpressionFunction.enhance("isEmpty('')"))
            .isEqualTo("test:isEmpty(scopeVariable,'')");
    }

    class TestNoPrefixFlowableShorthandExpressionFunction extends AbstractFlowableShortHandExpressionFunction {

        TestNoPrefixFlowableShorthandExpressionFunction() {
            super("scopeVariable", Arrays.asList("empty", "isEmpty"), "isEmpty");
        }

        @Override
        protected List<String> getFunctionPrefixOptions() {
            return functionPrefixOptions;
        }

        @Override
        protected String getFinalFunctionPrefix() {
            return finalFunctionPrefix;
        }

        @Override
        protected boolean isMultiParameterFunction() {
            return false;
        }

        public void isEmpty(String value) {

        }
    }

}
