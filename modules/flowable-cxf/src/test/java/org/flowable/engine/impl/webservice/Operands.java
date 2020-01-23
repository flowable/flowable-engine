package org.flowable.engine.impl.webservice;

public class Operands {

    private int arg1 = 0;

    private int arg2 = 0;

    public Operands() {
    }

    public Operands(final int arg1, final int arg2) {
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public int getArg1() {
        return this.arg1;
    }

    public void setArg1(final int arg1) {
        this.arg1 = arg1;
    }

    public int getArg2() {
        return this.arg2;
    }

    public void setArg2(final int arg2) {
        this.arg2 = arg2;
    }

}
