package com.ideas.rally;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class IterationCallBackTest{
    @BeforeClass
    public static void beforeClass(){
        RallyConfiguration.testRun=true;
    }

    @Test
    public void testIteration_iteration2() throws Exception {
        IterationCallBack callBack = new IterationCallBack();
        List input = new ArrayList();
        input.add("2014-01-12");
        List output = new ArrayList();
        callBack.procesResult(getIterationArray(),input,output);
        Assert.assertEquals("Iteration 2",output.get(0));
    }

    @Test
    public void testIteration_null() throws Exception {
        IterationCallBack callBack = new IterationCallBack();
        List input = new ArrayList();
        input.add("2014-01-30");
        List output = new ArrayList();
        callBack.procesResult(getIterationArray(),input,output);
        Assert.assertEquals(0,output.size());
    }

    private JsonArray getIterationArray(){
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getIteration("Iteration 1","2014-01-01T04:59:59.000Z","2014-01-10T04:59:59.000Z"));
        jsonArray.add(getIteration("Iteration 2","2014-01-11T04:59:59.000Z","2014-01-20T04:59:59.000Z"));
        jsonArray.add(getIteration("Iteration 3","2014-01-21T04:59:59.000Z","2014-01-30T04:59:59.000Z"));
        return jsonArray;
    }

    private JsonElement getIteration(String iterationName, String startDate, String endDate)
    {
        String jsonStr =
        "{"+
        " \"Name\":\""+iterationName+"\", "+
        " \"EndDate\":\""+endDate+"\","+
        " \"StartDate\":\""+startDate+"\""+
        "}";
        Gson gson = new Gson();
        return gson.fromJson (jsonStr, JsonElement.class);
    }

}