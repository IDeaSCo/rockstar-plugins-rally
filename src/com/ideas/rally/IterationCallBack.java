package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

public class IterationCallBack extends SFDCCallBack{
    @Override
    public void processResult(JsonArray array, List<String> input, List<String> output) throws Exception {
        for (JsonElement jsonElement : array) {
            String iterationName;
            JsonObject json = jsonElement.getAsJsonObject();
            iterationName = json.get("Name").getAsString();
            String startDate = json.get("StartDate").getAsString().substring(0, 10);
            String endDate = json.get("EndDate").getAsString().substring(0, 10);
            endDate = subtractOneDay(endDate);
            System.out.println("iterationName:" + iterationName + " startDate:" + startDate + " endDate:" + endDate);
            if (startDate.compareTo(input.get(0)) <= 0 && endDate.compareTo(input.get(0)) >= 0) {
                output.add(iterationName);
                output.add(startDate);
                break;
            }
        }
    }

}

