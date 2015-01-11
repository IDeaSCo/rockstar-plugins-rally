package com.ideas.rally;

import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class PreviousIterationCallBackTest extends IterationTestCase {
    private final PreviousIterationCallBack callBack = new PreviousIterationCallBack();

    @Test
    public void retrievePreviousIterationName() throws Exception {
        List<String> input = asList("2014-01-12");
        List<String> output = callBack.processResult(iterations, input);
        assertEquals(asList("Iteration 1"), output);
    }

    @Test
    public void retrievePreviousIterationNameEvenIfGivenDateIsLastDayOfIteration() throws Exception {
        List<String> input = asList("2014-01-28");
        List<String> output = callBack.processResult(iterations, input);
        assertEquals(asList("Iteration 2"), output);
    }

    @Test
    public void someDatesMightNotMatchAnyIteration() throws Exception {
        List<String> input = asList("2013-01-28");
        List<String> output = callBack.processResult(iterations, input);
        assertEquals(0, output.size());
    }
}