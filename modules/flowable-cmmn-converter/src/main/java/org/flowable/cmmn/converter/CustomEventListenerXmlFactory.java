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
package org.flowable.cmmn.converter;

import javax.xml.stream.XMLStreamReader;

import org.flowable.cmmn.model.EventListener;

/**
 * Builds an {@link EventListener} model object from an XML {@code <eventListener>} element whose
 * {@code flowable:eventType} attribute matches a key registered via
 * {@link GenericEventListenerXmlConverter#addCustomListenerTypeFactory}.
 * <p>
 * Common attributes (name, available-condition) are populated by {@code convertCommonAttributes} after the
 * factory returns; the {@code id} is set by {@code BaseCmmnXmlConverter.convertToCmmnModel}. The factory only
 * needs to read attributes specific to its custom listener type.
 */
@FunctionalInterface
public interface CustomEventListenerXmlFactory {

    EventListener create(XMLStreamReader xtr);
}
