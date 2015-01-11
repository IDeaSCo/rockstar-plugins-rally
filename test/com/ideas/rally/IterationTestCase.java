package com.ideas.rally;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.BeforeClass;

public class IterationTestCase {
    protected final JsonArray iterations= getIterationArray();

    @BeforeClass
    public static void beforeClass() {
        RallyConfiguration.testRun = true;
    }

    private JsonArray getIterationArray() {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getIteration("Iteration 1", "2014-01-01T04:59:59.000Z", "2014-01-10T04:59:59.000Z"));
        jsonArray.add(getIteration("Iteration 2", "2014-01-11T04:59:59.000Z", "2014-01-20T04:59:59.000Z"));
        jsonArray.add(getIteration("Iteration 3", "2014-01-21T04:59:59.000Z", "2014-01-30T04:59:59.000Z"));
        return jsonArray;
    }

    private JsonElement getIteration(String iterationName, String startDate, String endDate) {
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
