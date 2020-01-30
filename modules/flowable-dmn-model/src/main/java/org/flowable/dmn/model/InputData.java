package org.flowable.dmn.model;

/**
 * @author Yvo Swillens
 */
public class InputData extends NamedElement {

    protected InformationItem variable;

    public InformationItem getVariable() {
        return variable;
    }
    public void setVariable(InformationItem variable) {
        this.variable = variable;
    }
}
