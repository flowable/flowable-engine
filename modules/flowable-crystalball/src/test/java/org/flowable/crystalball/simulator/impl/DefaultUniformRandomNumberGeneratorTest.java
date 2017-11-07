package org.flowable.crystalball.simulator.impl;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

/**
 * This class tests {@link DefaultUniformRandomNumberGenerator} implementation
 */
public class DefaultUniformRandomNumberGeneratorTest {

    @Test
    public void testRandomNumberGenerator_interval() {
        // Arrange
        DefaultUniformRandomNumberGenerator uniformRandomNumberGenerator = new DefaultUniformRandomNumberGenerator();

        Map<Long, Integer> results = new HashMap();
        results.put(0l, 0);
        results.put(1l, 0);
        results.put(2l, 0);
        results.put(3l, 0);

        // Act
        for(int i = 0; i<10000; i++) {
            long randomNumber = uniformRandomNumberGenerator.getNumberFromInterval(0, 3);
            results.put(randomNumber, results.get(randomNumber) + 1);
        }

        // Assert
        assertThat(results.get(0l), Matchers.allOf(lessThan(2600), greaterThan(2400)));
        assertThat(results.get(1l), Matchers.allOf(lessThan(2600), greaterThan(2400)));
        assertThat(results.get(2l), Matchers.allOf(lessThan(2600), greaterThan(2400)));
        assertThat(results.get(3l), Matchers.allOf(lessThan(2600), greaterThan(2400)));
    }
}
