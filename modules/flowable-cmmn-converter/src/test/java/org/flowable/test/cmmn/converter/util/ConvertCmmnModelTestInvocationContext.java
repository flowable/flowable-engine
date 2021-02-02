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
package org.flowable.test.cmmn.converter.util;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.flowable.cmmn.model.CmmnModel;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

/**
 * @author Filip Hrisafov
 */
public class ConvertCmmnModelTestInvocationContext implements TestTemplateInvocationContext {

    protected final String name;
    protected final Supplier<CmmnModel> modelSupplier;

    public ConvertCmmnModelTestInvocationContext(String name, Supplier<CmmnModel> modelSupplier) {
        this.name = name;
        this.modelSupplier = modelSupplier;
    }

    @Override
    public String getDisplayName(int invocationIndex) {
        return name;
    }

    @Override
    public List<Extension> getAdditionalExtensions() {
        return Collections.singletonList(new ConvertCmmnModelResolver(modelSupplier));
    }

}
