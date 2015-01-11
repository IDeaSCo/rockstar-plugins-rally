package com.ideas.rally;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IterationCallBackTest extends IterationTestCase {
    private final IterationCallBack callBack = new IterationCallBack();

    @Test
    public void retrieveMatchingIterationNameAndStartDate() throws Exception {
        List<String> output = callBack.processResult(iterations, "2014-01-12");
        assertEquals(asList("Iteration 2", "2014-01-11"), output);
    }

    @Test
    public void givenDateCannotBeOnTheLastDayOfTheIteration() throws Exception {
        List<String> output = callBack.processResult(iterations, "2014-01-30");
        assertTrue(output.isEmpty());
    }
}