package com.ideas.rally;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;

public class PreviousIterationCallBackTest extends IterationTestCase {
    private final PreviousIterationCallBack callBack = new PreviousIterationCallBack();

    @Test
    public void testIteration_iteration1() throws Exception {
        List<String> input = asList("2014-01-12");
        callBack.processResult(iterations, input, output);
        Assert.assertEquals("Iteration 1",output.get(0));
    }

    @Test
    public void testIteration_iteration2() throws Exception {
        List<String> input = asList("2014-01-28");
        callBack.processResult(iterations, input, output);
        Assert.assertEquals("Iteration 2",output.get(0));
    }

    @Test
    public void testIteration_iteration_null() throws Exception {
        List<String> input = asList("2013-01-28");
        callBack.processResult(iterations, input, output);
        Assert.assertEquals(0,output.size());
    }
}