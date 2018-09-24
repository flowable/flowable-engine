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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CaseElement;
import org.flowable.cmmn.model.CmmnDiEdge;
import org.flowable.cmmn.model.CmmnDiShape;
import org.flowable.cmmn.model.CmmnElement;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.HasEntryCriteria;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryIfPart;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.common.engine.api.FlowableException;

/**
 * @author Joram Barrez
 */
public class ConversionHelper {

    protected CmmnModel cmmnModel;
    protected Case currentCase;
    protected LinkedList<CmmnElement> currentCmmnElements = new LinkedList<>();
    protected LinkedList<PlanFragment> planFragmentsStack = new LinkedList<>();
    protected LinkedList<Stage> stagesStack = new LinkedList<>();
    protected Sentry currentSentry;
    protected SentryOnPart currentSentryOnPart;
    protected PlanItem currentPlanItem;
    protected CmmnDiShape currentDiShape;
    protected CmmnDiEdge currentDiEdge;

    protected Map<Case, List<CaseElement>> caseElements = new HashMap<>();
    protected List<Stage> stages = new ArrayList<>();
    protected List<PlanFragment> planFragments = new ArrayList<>();
    protected List<Criterion> entryCriteria = new ArrayList<>();
    protected List<Criterion> exitCriteria = new ArrayList<>();
    protected List<Sentry> sentries = new ArrayList<>();
    protected List<SentryOnPart> sentryOnParts = new ArrayList<>();
    protected List<SentryIfPart> sentryIfParts = new ArrayList<>();
    protected List<PlanItem> planItems = new ArrayList<>();
    protected List<PlanItemDefinition> planItemDefinitions = new ArrayList<>();

    protected List<CmmnDiShape> diShapes = new ArrayList<>();
    protected List<CmmnDiEdge> diEdges = new ArrayList<>();

    public void addCaseElement(CaseElement caseElement) {
        if (!caseElements.containsKey(currentCase)) {
            caseElements.put(currentCase, new ArrayList<>());
        }
        caseElements.get(currentCase).add(caseElement);
    }

    public Map<Case, List<CaseElement>> getCaseElements() {
        return caseElements;
    }

    public void setCaseElements(Map<Case, List<CaseElement>> caseElements) {
        this.caseElements = caseElements;
    }

    public void addStage(Stage stage) {
        stages.add(stage);
    }

    public void addPlanFragment(PlanFragment planFragment) {
        planFragments.add(planFragment);
    }

    public void addEntryCriterion(Criterion entryCriterion) {
        entryCriteria.add(entryCriterion);
    }

    public void addEntryCriterionToCurrentElement(Criterion entryCriterion) {
        addEntryCriterion(entryCriterion);

        ListIterator<CmmnElement> iterator = currentCmmnElements.listIterator(currentCmmnElements.size());
        HasEntryCriteria hasEntryCriteria = null;
        while (hasEntryCriteria == null && iterator.hasPrevious()) {
            CmmnElement cmmnElement = iterator.previous();
            if (cmmnElement instanceof HasEntryCriteria) {
                hasEntryCriteria = (HasEntryCriteria) cmmnElement;
                entryCriterion.setAttachedToRefId(cmmnElement.getId());
            }
        }
        if (hasEntryCriteria != null) {
            hasEntryCriteria.getEntryCriteria().add(entryCriterion);
        } else {
            throw new FlowableException("Cannot add an entry criteria " + entryCriterion.getId() + " no matching plan item found to attach it to");
        }
    }

    public void addExitCriterion(Criterion exitCriterion) {
        exitCriteria.add(exitCriterion);
    }

    public void addExitCriteriaToCurrentElement(Criterion exitCriterion) {
        addExitCriterion(exitCriterion);

        ListIterator<CmmnElement> iterator = currentCmmnElements.listIterator(currentCmmnElements.size());
        HasExitCriteria hasExitCriteria = null;
        while (hasExitCriteria == null && iterator.hasPrevious()) {
            CmmnElement cmmnElement = iterator.previous();
            if (cmmnElement instanceof HasExitCriteria) {
                hasExitCriteria = (HasExitCriteria) cmmnElement;
                exitCriterion.setAttachedToRefId(cmmnElement.getId());
            }
        }

        if (hasExitCriteria != null) {
            hasExitCriteria.getExitCriteria().add(exitCriterion);
        } else {
            getCurrentCase().getPlanModel().getExitCriteria().add(exitCriterion);
        }
    }

    public void addSentry(Sentry sentry) {
        sentries.add(sentry);
    }

    public void addSentryToCurrentPlanFragment(Sentry sentry) {
        addSentry(sentry);
        setCurrentSentry(sentry);
        getCurrentPlanFragment().addSentry(sentry);
    }

    public void addSentryOnPart(SentryOnPart sentryOnPart) {
        sentryOnParts.add(sentryOnPart);
    }

    public void addSentryOnPartToCurrentSentry(SentryOnPart sentryOnPart) {
        addSentryOnPart(sentryOnPart);
        getCurrentSentry().addSentryOnPart(sentryOnPart);
        setCurrentSentryOnPart(sentryOnPart);
    }

    public void addSentryIfPart(SentryIfPart sentryIfPart) {
        sentryIfParts.add(sentryIfPart);
    }

    public void addSentryIfPartToCurrentSentry(SentryIfPart sentryIfPart) {
        addSentryIfPart(sentryIfPart);
        getCurrentSentry().setSentryIfPart(sentryIfPart);
    }

    public void addPlanItem(PlanItem planItem) {
        planItems.add(planItem);
    }

    public void addPlanItemToCurrentPlanFragment(PlanItem planItem) {
        addPlanItem(planItem);
        getCurrentPlanFragment().addPlanItem(planItem);
        setCurrentPlanItem(planItem);
    }

    public void addPlanItemDefinition(PlanItemDefinition planItemDefinition) {
        planItemDefinitions.add(planItemDefinition);
    }

    public void addDiShape(CmmnDiShape diShape) {
        diShapes.add(diShape);
        setCurrentDiShape(diShape);
    }

    public void addDiEdge(CmmnDiEdge diEdge) {
        diEdges.add(diEdge);
        setCurrentDiEdge(diEdge);
    }

    public CmmnModel getCmmnModel() {
        return cmmnModel;
    }

    public void setCmmnModel(CmmnModel cmmnModel) {
        this.cmmnModel = cmmnModel;
    }

    public Case getCurrentCase() {
        return currentCase;
    }

    public void setCurrentCase(Case currentCase) {
        this.currentCase = currentCase;
    }

    public CmmnElement getCurrentCmmnElement() {
        return currentCmmnElements.peekLast();
    }

    public PlanFragment getCurrentPlanFragment() {
        return planFragmentsStack.peekLast();
    }

    public void setCurrentPlanFragment(PlanFragment currentPlanFragment) {
        if (currentPlanFragment != null) {
            this.planFragmentsStack.add(currentPlanFragment);
        }
    }

    public void removeCurrentPlanFragment() {
        this.planFragmentsStack.removeLast();
    }

    public Stage getCurrentStage() {
        return stagesStack.peekLast();
    }

    public void setCurrentStage(Stage currentStage) {
        if (currentStage != null) {
            this.stagesStack.add(currentStage);
            setCurrentPlanFragment(currentStage);
        }
    }

    public void removeCurrentStage() {
        this.stagesStack.removeLast();
        removeCurrentPlanFragment();
    }

    public void setCurrentCmmnElement(CmmnElement currentCmmnElement) {
        currentCmmnElements.add(currentCmmnElement);
    }

    public void removeCurrentCmmnElement() {
        currentCmmnElements.removeLast();
    }

    public Sentry getCurrentSentry() {
        return currentSentry;
    }

    public void setCurrentSentry(Sentry currentSentry) {
        this.currentSentry = currentSentry;
    }

    public SentryOnPart getCurrentSentryOnPart() {
        return currentSentryOnPart;
    }

    public void setCurrentSentryOnPart(SentryOnPart currentSentryOnPart) {
        this.currentSentryOnPart = currentSentryOnPart;
    }

    public PlanItem getCurrentPlanItem() {
        return currentPlanItem;
    }

    public void setCurrentPlanItem(PlanItem currentPlanItem) {
        this.currentPlanItem = currentPlanItem;
    }

    public CmmnDiShape getCurrentDiShape() {
        return currentDiShape;
    }

    public void setCurrentDiShape(CmmnDiShape currentDiShape) {
        this.currentDiShape = currentDiShape;
    }

    public CmmnDiEdge getCurrentDiEdge() {
        return currentDiEdge;
    }

    public void setCurrentDiEdge(CmmnDiEdge currentDiEdge) {
        this.currentDiEdge = currentDiEdge;
    }

    public List<Stage> getStages() {
        return stages;
    }

    public List<PlanFragment> getPlanFragments() {
        return planFragments;
    }

    public List<Criterion> getEntryCriteria() {
        return entryCriteria;
    }

    public List<Criterion> getExitCriteria() {
        return exitCriteria;
    }

    public List<Sentry> getSentries() {
        return sentries;
    }

    public List<SentryOnPart> getSentryOnParts() {
        return sentryOnParts;
    }

    public List<SentryIfPart> getSentryIfParts() {
        return sentryIfParts;
    }

    public List<PlanItem> getPlanItems() {
        return planItems;
    }

    public List<PlanItemDefinition> getPlanItemDefinitions() {
        return planItemDefinitions;
    }

    public List<CmmnDiShape> getDiShapes() {
        return diShapes;
    }

    public List<CmmnDiEdge> getDiEdges() {
        return diEdges;
    }

}
