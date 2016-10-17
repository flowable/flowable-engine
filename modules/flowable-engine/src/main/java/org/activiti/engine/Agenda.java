package org.activiti.engine;

/**
 * This interface exposes independent agenda methods.
 */
public interface Agenda {
    boolean isEmpty();

    Runnable getNextOperation();

    void planOperation(Runnable operation);
}
