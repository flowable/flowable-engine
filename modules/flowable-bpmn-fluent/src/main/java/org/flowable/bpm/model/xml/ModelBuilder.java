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
package org.flowable.bpm.model.xml;

import org.flowable.bpm.model.xml.impl.ModelBuilderImpl;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;

/**
 * This builder is used to define and create a new model.
 */
public abstract class ModelBuilder {

    public abstract ModelBuilder alternativeNamespace(String alternativeNs, String actualNs);

    public abstract ModelElementTypeBuilder defineType(Class<? extends ModelElementInstance> modelInstanceType, String typeName);

    public abstract ModelElementType defineGenericType(String typeName, String typeNamespaceUri);

    public abstract Model build();

    public static ModelBuilder createInstance(String modelName) {
        return new ModelBuilderImpl(modelName);
    }

}
