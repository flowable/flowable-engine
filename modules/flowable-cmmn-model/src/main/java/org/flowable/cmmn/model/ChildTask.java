package org.flowable.cmmn.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Valentin Zickner
 */
public class ChildTask extends Task {

    protected List<IOParameter> inParameters = new ArrayList<>();
    protected List<IOParameter> outParameters = new ArrayList<>();

    public List<IOParameter> getInParameters() {
        return inParameters;
    }

    public void setInParameters(List<IOParameter> inParameters) {
        this.inParameters = inParameters;
    }

    public List<IOParameter> getOutParameters() {
        return outParameters;
    }

    public void setOutParameters(List<IOParameter> outParameters) {
        this.outParameters = outParameters;
    }

}
