package org.flowable.crystalball.simulator.impl;

import org.flowable.crystalball.simulator.RandomNumberGenerator;

import java.util.Random;

/**
 * Random number generator with an uniform distribution based on the java random number generator.
 */
public class DefaultUniformRandomNumberGenerator implements RandomNumberGenerator {

    protected Random random;

    public DefaultUniformRandomNumberGenerator() {
        this.random = new Random();
    }

    public DefaultUniformRandomNumberGenerator(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public long getNumberFromInterval(long min, long max) {
        return ((long) (this.random.nextDouble() * (max - min + 1))) + min;
    }
}
