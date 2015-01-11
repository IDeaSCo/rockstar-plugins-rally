package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import static com.ideas.rally.IterationCallBack.extract;
import static com.ideas.rally.IterationCallBack.inRange;

public class PreviousIterationCallBack extends SFDCCallBack{
    @Override
    public List<String> processResult(JsonArray iterations, List<String> input) throws Exception {
        List<String> allIterations = new ArrayList<String>();
        List<String> results = new ArrayList<String>();
        for (JsonElement element : iterations) {
            JsonObject iteration = element.getAsJsonObject();
            String iterationName = iteration.get("Name").getAsString();
            String startDate = extract("StartDate", iteration);
            String endDate = extract("EndDate", iteration);
            String givenDate = input.get(0);

            if (inRange(givenDate, startDate, endDate)) {
                if(allIterations.size() > 0 ) {
                    results.add(tail(allIterations));
                    return results;
                }
            }
            allIterations.add(iterationName);
        }
        return results;
    }

    private String tail(List<String> list) {
        return list.get(list.size() - 1);
    }

}

