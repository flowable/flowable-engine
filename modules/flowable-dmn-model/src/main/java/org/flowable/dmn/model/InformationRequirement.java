package org.flowable.dmn.model;

/**
 * @author Yvo Swillens
 */
public class InformationRequirement extends NamedElement {

    protected DmnElementReference requiredDecision;
    protected DmnElementReference requiredInput;

    public DmnElementReference getRequiredDecision() {
        return requiredDecision;
    }
    public void setRequiredDecision(DmnElementReference requiredDecision) {
        this.requiredDecision = requiredDecision;
    }
    public DmnElementReference getRequiredInput() {
        return requiredInput;
    }
    public void setRequiredInput(DmnElementReference requiredInput) {
        this.requiredInput = requiredInput;
    }
}
