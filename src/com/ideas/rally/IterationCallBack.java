package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

public class IterationCallBack extends SFDCCallBack{
    @Override
    public void processResult(JsonArray array, List<String> input, List<String> output) throws Exception {
        for (JsonElement jsonElement : array) {
            JsonObject iteration = jsonElement.getAsJsonObject();
            String iterationName = iteration.get("Name").getAsString();
            String startDate = extract("StartDate", iteration);
            String endDate = subtractOneDay(extract("EndDate", iteration));
            String givenDate = input.get(0);
            if (withInRange(givenDate, startDate, endDate)) {
                output.add(iterationName);
                output.add(startDate);
                return;
            }
        }
    }

    private boolean withInRange(String givenDate, String startDate, String endDate) {
        return startDate.compareTo(givenDate) <= 0 && endDate.compareTo(givenDate) >= 0;
    }

    private String extract(String date, JsonObject json) {
        return json.get(date).getAsString().substring(0, 10);
    }

}

