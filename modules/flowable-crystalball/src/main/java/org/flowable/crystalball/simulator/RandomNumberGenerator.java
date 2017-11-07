package org.flowable.crystalball.simulator;

/**
 * Generate a pseudo random numbers
 *
 * @author martin.grofcik
 */
public interface RandomNumberGenerator {

    /**
     * Generate a random number from <min, max> interval.
     * @param min min value included
     * @param max max value included
     * @return a random number from the interval
     */
    long getNumberFromInterval(long min, long max);
}
