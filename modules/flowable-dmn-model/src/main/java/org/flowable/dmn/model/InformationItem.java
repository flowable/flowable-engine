package org.flowable.dmn.model;

/**
 * @author Yvo Swillens
 */
public class InformationItem extends NamedElement {

    protected String typeRef;

    public String getTypeRef() {
        return typeRef;
    }
    public void setTypeRef(String typeRef) {
        this.typeRef = typeRef;
    }
}
