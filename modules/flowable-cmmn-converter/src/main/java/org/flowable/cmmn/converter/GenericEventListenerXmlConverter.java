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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.model.BaseElement;
import org.flowable.cmmn.model.EventListener;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.IntentEventListener;
import org.flowable.cmmn.model.ReactivateEventListener;
import org.flowable.cmmn.model.SignalEventListener;
import org.flowable.cmmn.model.VariableEventListener;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class GenericEventListenerXmlConverter extends PlanItemDefinitionXmlConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericEventListenerXmlConverter.class);

    /**
     * Registry of {@code flowable:eventListenerType} discriminator values to factories that build the typed
     * {@link EventListener} model object. Pre-populated with the four built-in discriminators ("signal",
     * "variable", "intent", "reactivate"); additional types are added via
     * {@link #addCustomListenerTypeFactory(String, CustomEventListenerXmlFactory)}.
     * <p>
     * <b>Lifecycle:</b> JVM-wide static, shared by every {@link CmmnXmlConverter} instance in the process.
     * Registration is last-writer-wins for added types; the four built-in keys are reserved and cannot be
     * overridden. Tests that build multiple engines should call
     * {@link #removeCustomListenerTypeFactory(String)} between runs to keep the registry clean.
     */
    private static final Map<String, CustomEventListenerXmlFactory> LISTENER_TYPE_FACTORIES = new HashMap<>();

    private static final Set<String> BUILT_IN_LISTENER_TYPES = Set.of("signal", "variable", "intent", "reactivate");

    static {
        LISTENER_TYPE_FACTORIES.put("signal", xtr -> {
            SignalEventListener listener = new SignalEventListener();
            listener.setSignalRef(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_SIGNAL_REF));
            return listener;
        });
        LISTENER_TYPE_FACTORIES.put("variable", xtr -> {
            VariableEventListener listener = new VariableEventListener();
            listener.setVariableName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_VARIABLE_NAME));
            listener.setVariableChangeType(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_VARIABLE_CHANGE_TYPE));
            return listener;
        });
        LISTENER_TYPE_FACTORIES.put("intent", xtr -> new IntentEventListener());
        LISTENER_TYPE_FACTORIES.put("reactivate", xtr -> new ReactivateEventListener());
    }

    public static void addCustomListenerTypeFactory(String listenerType, CustomEventListenerXmlFactory factory) {
        if (BUILT_IN_LISTENER_TYPES.contains(listenerType)) {
            throw new FlowableIllegalArgumentException("Cannot override built-in eventListenerType '" + listenerType + "'");
        }
        LISTENER_TYPE_FACTORIES.put(listenerType, factory);
    }

    public static void removeCustomListenerTypeFactory(String listenerType) {
        if (BUILT_IN_LISTENER_TYPES.contains(listenerType)) {
            return;
        }
        LISTENER_TYPE_FACTORIES.remove(listenerType);
    }

    @Override
    public boolean hasChildElements() {
        return true;
    }

    @Override
    public String getXMLElementName() {
        return CmmnXmlConstants.ELEMENT_GENERIC_EVENT_LISTENER;
    }

    @Override
    protected BaseElement convert(XMLStreamReader xtr, ConversionHelper conversionHelper) {
        String listenerType = xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_TYPE);
        CustomEventListenerXmlFactory factory = listenerType != null ? LISTENER_TYPE_FACTORIES.get(listenerType) : null;
        if (factory != null) {
            return convertCommonAttributes(xtr, factory.create(xtr));
        }

        if (StringUtils.isNotEmpty(listenerType)) {
            // listenerType is set but matches no registered factory — likely a typo or missing
            // addCustomEventListenerTypeFactory registration. Falling through to GenericEventListener would
            // silently produce a non-firing listener; warn so the misconfiguration surfaces at parse time.
            LOGGER.warn("Unrecognized flowable:eventType '{}' on eventListener '{}'; falling back to a generic event listener. Registered types: {}",
                    listenerType, xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_ID), LISTENER_TYPE_FACTORIES.keySet());
        }

        return convertCommonAttributes(xtr, new GenericEventListener());
    }

    protected EventListener convertCommonAttributes(XMLStreamReader xtr, EventListener listener) {
        listener.setName(xtr.getAttributeValue(null, CmmnXmlConstants.ATTRIBUTE_NAME));
        listener.setAvailableConditionExpression(xtr.getAttributeValue(CmmnXmlConstants.FLOWABLE_EXTENSIONS_NAMESPACE,
            CmmnXmlConstants.ATTRIBUTE_EVENT_LISTENER_AVAILABLE_CONDITION));
        return listener;
    }

}
