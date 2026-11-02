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
package org.flowable.bpmn.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.bpmn.model.EventDefinitionLocation.BOUNDARY_EVENT;
import static org.flowable.bpmn.model.EventDefinitionLocation.END_EVENT;
import static org.flowable.bpmn.model.EventDefinitionLocation.EVENT_SUBPROCESS_START_EVENT;
import static org.flowable.bpmn.model.EventDefinitionLocation.INTERMEDIATE_CATCH_EVENT;
import static org.flowable.bpmn.model.EventDefinitionLocation.INTERMEDIATE_THROW_EVENT;
import static org.flowable.bpmn.model.EventDefinitionLocation.START_EVENT;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class EventDefinitionSupportedLocationsTest {

    static Stream<Arguments> supportedLocationsPerSubtype() {
        return Stream.of(
                Arguments.of(supplier(CancelEventDefinition::new),
                        EnumSet.of(BOUNDARY_EVENT, END_EVENT)),
                Arguments.of(supplier(CompensateEventDefinition::new),
                        EnumSet.of(BOUNDARY_EVENT, INTERMEDIATE_THROW_EVENT)),
                Arguments.of(supplier(ConditionalEventDefinition::new),
                        EnumSet.of(EVENT_SUBPROCESS_START_EVENT, INTERMEDIATE_CATCH_EVENT, BOUNDARY_EVENT)),
                Arguments.of(supplier(ErrorEventDefinition::new),
                        EnumSet.of(EVENT_SUBPROCESS_START_EVENT, BOUNDARY_EVENT, END_EVENT)),
                Arguments.of(supplier(EscalationEventDefinition::new),
                        EnumSet.of(EVENT_SUBPROCESS_START_EVENT, BOUNDARY_EVENT, END_EVENT, INTERMEDIATE_THROW_EVENT)),
                Arguments.of(supplier(EventRegistryEventDefinition::new),
                        EnumSet.of(START_EVENT, EVENT_SUBPROCESS_START_EVENT, INTERMEDIATE_CATCH_EVENT, BOUNDARY_EVENT)),
                Arguments.of(supplier(MessageEventDefinition::new),
                        EnumSet.of(START_EVENT, EVENT_SUBPROCESS_START_EVENT, INTERMEDIATE_CATCH_EVENT, BOUNDARY_EVENT)),
                Arguments.of(supplier(SignalEventDefinition::new),
                        EnumSet.of(START_EVENT, EVENT_SUBPROCESS_START_EVENT, INTERMEDIATE_CATCH_EVENT, BOUNDARY_EVENT, INTERMEDIATE_THROW_EVENT)),
                Arguments.of(supplier(TerminateEventDefinition::new),
                        EnumSet.of(END_EVENT)),
                Arguments.of(supplier(TimerEventDefinition::new),
                        EnumSet.of(START_EVENT, EVENT_SUBPROCESS_START_EVENT, INTERMEDIATE_CATCH_EVENT, BOUNDARY_EVENT)),
                Arguments.of(supplier(VariableListenerEventDefinition::new),
                        EnumSet.of(EVENT_SUBPROCESS_START_EVENT, INTERMEDIATE_CATCH_EVENT, BOUNDARY_EVENT)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedLocationsPerSubtype")
    void declaresExpectedSupportedLocations(Supplier<EventDefinition> factory, Set<EventDefinitionLocation> expected) {
        EventDefinition definition = factory.get();
        assertThat(definition.getSupportedLocations()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("supportedLocationsPerSubtype")
    void supportedLocationsIsImmutable(Supplier<EventDefinition> factory) {
        Set<EventDefinitionLocation> locations = factory.get().getSupportedLocations();
        assertThat(locations).isUnmodifiable();
    }

    private static Supplier<EventDefinition> supplier(Supplier<? extends EventDefinition> factory) {
        Class<?> type = factory.get().getClass();
        return new Supplier<>() {

            @Override
            public EventDefinition get() {
                return factory.get();
            }

            @Override
            public String toString() {
                return type.getSimpleName();
            }
        };
    }
}
