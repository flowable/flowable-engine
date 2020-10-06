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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.InputStream;
import java.util.List;

import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnXmlConverterTest extends AbstractConverterTest {

    private CmmnXmlConverter cmmnXmlConverter;

    @Before
    public void setup() {
        this.cmmnXmlConverter = new CmmnXmlConverter();
    }

    /**
     * Test simple case model, with 4 consequent elements: taskA -> milestone 1 -> taskB -> milestone 2.
     * <p>
     * The converters should check following model class instances:
     * - 1 case
     * - 1 stage (the plan model)
     * - 4 plan item definitions (one for each plan item)
     * - 4 plan items: 2 tasks and 2 milestones
     * - 4 sentries
     * - 3 entry criteria (on all plan items except taskA)
     */
    @Test
    public void testSimpleCmmnModelConversion() {
        CmmnModel cmmnModel = readXMLFile("org/flowable/test/cmmn/converter/simple-case.cmmn");
        validateSimpleCaseModel(cmmnModel);
    }

    @Test
    public void testSimpleCmmnModelDoubleConversion() {
        CmmnModel cmmnModel = readXMLFile("org/flowable/test/cmmn/converter/simple-case.cmmn");
        CmmnModel parsedModel = exportAndReadXMLFile(cmmnModel);
        validateSimpleCaseModel(parsedModel);
    }

    protected void validateSimpleCaseModel(CmmnModel cmmnModel) {
        assertThat(cmmnModel).isNotNull();

        // Case
        assertThat(cmmnModel.getCases())
                .extracting(Case::getId)
                .containsExactly("myCase");

        // Plan model
        Stage planModel = cmmnModel.getCases().get(0).getPlanModel();
        assertThat(cmmnModel.getCases())
                .extracting(cases -> cases.getPlanModel().getId(), cases -> cases.getPlanModel().getName(), cases -> cases.getPlanModel().getFormKey())
                .containsExactly(tuple("myPlanModel", "My CasePlanModel", "casePlanForm"));

        // Sentries
        assertThat(planModel.getSentries()).hasSize(4);
        for (Sentry sentry : planModel.getSentries()) {
            List<SentryOnPart> onParts = sentry.getOnParts();
            if (onParts != null && !onParts.isEmpty()) {
                assertThat(onParts)
                        .hasSize(1)
                        .extracting(SentryOnPart::getId, SentryOnPart::getSourceRef, SentryOnPart::getSource, SentryOnPart::getStandardEvent)
                        .doesNotContainNull();
            } else {
                assertThat(sentry.getSentryIfPart().getCondition()).isEqualTo("${true}");
                assertThat(sentry.getName()).isEqualTo("criterion name");
            }
        }

        // Plan items definitions
        List<PlanItemDefinition> planItemDefinitions = planModel.getPlanItemDefinitions();
        assertThat(planItemDefinitions).hasSize(4);
        assertThat(planModel.findPlanItemDefinitionsOfType(Task.class, false)).hasSize(2);
        assertThat(planModel.findPlanItemDefinitionsOfType(Milestone.class, false)).hasSize(2);
        for (PlanItemDefinition planItemDefinition : planItemDefinitions) {
            assertThat(planItemDefinition.getId()).isNotNull();
            assertThat(planItemDefinition.getName()).isNotNull();
        }

        // Plan items
        List<PlanItem> planItems = planModel.getPlanItems();
        assertThat(planItems).hasSize(4);
        int nrOfTasks = 0;
        int nrOfMileStones = 0;
        for (PlanItem planItem : planItems) {
            assertThat(planItem.getId()).isNotNull();
            assertThat(planItem.getDefinitionRef()).isNotNull();
            assertThat(planItem.getPlanItemDefinition()).isNotNull(); // Verify plan item definition ref is resolved

            PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
            if (planItemDefinition instanceof Milestone) {
                nrOfMileStones++;
            } else if (planItemDefinition instanceof Task) {
                nrOfTasks++;
            }

            if (!planItem.getId().equals("planItemTaskA")) {
                assertThat(planItem.getEntryCriteria())
                        .hasSize(1)
                        .extracting(Criterion::getSentry)
                        .isNotNull(); // Verify if sentry reference is resolved
            }

            if (planItem.getPlanItemDefinition() instanceof Task) {
                if (planItem.getId().equals("planItemTaskB")) {
                    assertThat(((Task) planItem.getPlanItemDefinition()).isBlocking()).isFalse();
                } else {
                    assertThat(((Task) planItem.getPlanItemDefinition()).isBlocking()).isTrue();
                }
            }
        }

        assertThat(nrOfMileStones).isEqualTo(2);
        assertThat(nrOfTasks).isEqualTo(2);
    }

    /**
     * Same case model as in {@link #testSimpleCmmnModelConversion()}, but now with an exit criteria on the plan model.
     */
    @Test
    public void testExitCriteriaOnPlanModel() {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("exit-criteria-on-planmodel.cmmn"));
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertThat(planModel.getSentries()).hasSize(4);

        List<Criterion> exitCriteria = planModel.getExitCriteria();
        assertThat(exitCriteria).hasSize(1);
        Criterion criterion = exitCriteria.get(0);
        assertThat(criterion.getSentry()).isNotNull();
        assertThat(criterion.getSentry().getOnParts().get(0).getSource().getId()).isEqualTo("planItemMileStoneOne");
    }

    @Test
    public void testNestedStages() {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("nested-stages.cmmn"));
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertThat(planModel.getPlanItems()).hasSize(2);

        Stage nestedStage = null;
        for (PlanItem planItem : planModel.getPlanItems()) {
            assertThat(planItem.getPlanItemDefinition()).isNotNull();
            if (planItem.getPlanItemDefinition() instanceof Stage) {
                nestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertThat(nestedStage).isNotNull();
        assertThat(nestedStage.getName()).isEqualTo("Nested Stage");

        // Nested stage has 3 plan items, and one of them references the rootTook from the plan model
        assertThat(nestedStage.getPlanItems()).hasSize(3);
        Stage nestedNestedStage = null;
        for (PlanItem planItem : nestedStage.getPlanItems()) {
            assertThat(planItem.getPlanItemDefinition()).isNotNull();
            if (planItem.getPlanItemDefinition() instanceof Stage) {
                nestedNestedStage = (Stage) planItem.getPlanItemDefinition();
            }
        }
        assertThat(nestedNestedStage).isNotNull();
        assertThat(nestedNestedStage.getName()).isEqualTo("Nested Stage 2");
        assertThat(nestedNestedStage.getPlanItems())
                .extracting(planItem -> planItem.getPlanItemDefinition().getId())
                .containsExactly("rootTask");
    }

    @Test
    public void testCaseLifecycleListener() throws Exception {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("case-lifecycle-listeners.cmmn"));
        cmmnModel = exportAndReadXMLFile(cmmnModel);

        assertThat(cmmnModel.getCases()).hasSize(1);
        Case aCase = cmmnModel.getCases().get(0);
        assertThat(aCase.getLifecycleListeners())
                .extracting(FlowableListener::getSourceState, FlowableListener::getTargetState, FlowableListener::getImplementationType,
                        FlowableListener::getImplementation)
                .containsExactly(
                        tuple("active", "completed", ImplementationType.IMPLEMENTATION_TYPE_EXPRESSION, "${caseInstance.setVariable('stageThree', false)}"));
    }

    @Test
    public void testMissingIdsAdded() {
        CmmnModel cmmnModel = cmmnXmlConverter.convertToCmmnModel(getInputStreamProvider("exit-criteria-on-planmodel.cmmn"));
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        assertThat(planModel.getId()).isNotNull();

        for (Sentry sentry : planModel.getSentries()) {
            assertThat(sentry.getId()).isNotNull();
            for (SentryOnPart onPart : sentry.getOnParts()) {
                assertThat(onPart.getId()).isNotNull();
            }
        }
    }

    private InputStreamProvider getInputStreamProvider(final String resourceName) {
        return new InputStreamProvider() {

            @Override
            public InputStream getInputStream() {
                return this.getClass().getClassLoader().getResourceAsStream(
                        this.getClass().getPackage().getName().replaceAll("\\.", "/") + "/" + resourceName);
            }

        };
    }

}
