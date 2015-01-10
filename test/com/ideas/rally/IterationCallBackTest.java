package com.ideas.rally;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IterationCallBackTest extends IterationTestCase {
    private final IterationCallBack callBack = new IterationCallBack();

    @Test
    public void retrieveMatchingIterationNameAndStartDate() throws Exception {
        callBack.processResult(iterations, asList("2014-01-12"), output);
        assertEquals(asList("Iteration 2", "2014-01-11"), output);
    }

    @Test
    public void givenDateCannotBeOnTheLastDayOfTheIteration() throws Exception {
        callBack.processResult(iterations, asList("2014-01-30"), output);
        assertTrue(output.isEmpty());
    }
}