package com.ideas.rally;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IterationCallBackTest {
    private static final JsonArray iterations = getIterationArray();
    private final CallBack currentIterationCallBack = new IterationCallBack(new CurrentIteration());
    private final CallBack lastIterationCallBack = new IterationCallBack(new LastIteration());

    @BeforeClass
    public static void beforeClass() {
        RallyConfiguration.testRun = true;
    }

    @Test
    public void retrieveMatchingIterationNameAndStartDate() throws Exception {
        List<String> output = currentIterationCallBack.processResult(iterations, "2014-01-12");
        assertEquals(asList("Iteration 2", "2014-01-11"), output);
    }

    @Test
    public void givenDateCannotBeOnTheLastDayOfTheIteration() throws Exception {
        List<String> output = currentIterationCallBack.processResult(iterations, "2014-01-30");
        assertTrue(output.isEmpty());
    }

    @Test
    public void retrievePreviousIterationName() throws Exception {
        List<String> output = lastIterationCallBack.processResult(iterations, "2014-01-12");
        assertEquals(asList("Iteration 1"), output);
    }

    @Test
    public void retrievePreviousIterationNameEvenIfGivenDateIsLastDayOfIteration() throws Exception {
        List<String> output = lastIterationCallBack.processResult(iterations, "2014-01-28");
        assertEquals(asList("Iteration 2"), output);
    }

    @Test
    public void someDatesMightNotMatchAnyIteration() throws Exception {
        List<String> output = lastIterationCallBack.processResult(iterations, "2013-01-28");
        assertEquals(0, output.size());
    }

    private static JsonArray getIterationArray() {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getIteration("Iteration 1", "2014-01-01T04:59:59.000Z", "2014-01-10T04:59:59.000Z"));
        jsonArray.add(getIteration("Iteration 2", "2014-01-11T04:59:59.000Z", "2014-01-20T04:59:59.000Z"));
        jsonArray.add(getIteration("Iteration 3", "2014-01-21T04:59:59.000Z", "2014-01-30T04:59:59.000Z"));
        return jsonArray;
    }

    private static JsonElement getIteration(String iterationName, String startDate, String endDate) {
        String jsonStr =
                "{" +
                        " \"Name\":\"" + iterationName + "\", " +
                        " \"EndDate\":\"" + endDate + "\"," +
                        " \"StartDate\":\"" + startDate + "\"" +
                        "}";
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, JsonElement.class);
    }
}