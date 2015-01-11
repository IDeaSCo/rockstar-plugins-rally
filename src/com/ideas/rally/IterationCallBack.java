package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class IterationCallBack extends SFDCCallBack{
    @Override
    public List<String>  processResult(JsonArray iterations, String... input) throws Exception {
        List<String> output = new ArrayList<String>();
        for (JsonElement element : iterations) {
            JsonObject iteration = element.getAsJsonObject();
            String iterationName = iteration.get("Name").getAsString();
            String startDate = extract("StartDate", iteration);
            String endDate = subtractOneDay(extract("EndDate", iteration));
            String givenDate = input[0];
            if (inRange(givenDate, startDate, endDate)) {
                output.add(iterationName);
                output.add(startDate);
                break;
            }
        }
        return output;
    }

    static boolean inRange(String givenDate, String startDate, String endDate) {
        return startDate.compareTo(givenDate) <= 0 && endDate.compareTo(givenDate) >= 0;
    }

    static String extract(String date, JsonObject json) {
        return json.get(date).getAsString().substring(0, 10);
    }

}

