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

import java.util.function.Supplier;

import org.flowable.cmmn.model.CmmnModel;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * @author Filip Hrisafov
 */
public class ConvertCmmnModelResolver implements ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ConvertCmmnModelResolver.class);

    protected final Supplier<CmmnModel> modelSupplier;

    public ConvertCmmnModelResolver(Supplier<CmmnModel> modelSupplier) {
        this.modelSupplier = modelSupplier;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return CmmnModel.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        ExtensionContext.Store store = extensionContext.getRoot().getStore(NAMESPACE);

        return store.getOrComputeIfAbsent(extensionContext.getUniqueId(), key -> modelSupplier.get(), CmmnModel.class);
    }

}
