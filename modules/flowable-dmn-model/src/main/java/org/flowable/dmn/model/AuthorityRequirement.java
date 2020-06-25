package org.flowable.dmn.model;

/**
 * @author Yvo Swillens
 */
public class AuthorityRequirement extends NamedElement {

    protected DmnElementReference requiredDecision;
    protected DmnElementReference requiredInput;
    protected DmnElementReference requiredAuthority;

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
    public DmnElementReference getRequiredAuthority() {
        return requiredAuthority;
    }
    public void setRequiredAuthority(DmnElementReference requiredAuthority) {
        this.requiredAuthority = requiredAuthority;
    }
}
