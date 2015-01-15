package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;

public class IterationCallBack extends CallBack {
    private final IterationType iterationType;

    public IterationCallBack(IterationType iterationType) {
        this.iterationType = iterationType;
    }

    @Override
    public List<String>  processResult(JsonArray iterations, String... input) throws Exception {
        for (JsonElement element : iterations) {
            JsonObject iteration = element.getAsJsonObject();
            String iterationName = iteration.get("Name").getAsString();
            String startDate = extract("StartDate", iteration);
            String endDate = subtractOneDay(extract("EndDate", iteration));
            String givenDate = input[0];
            if(iterationType.shouldStop(iterationName, givenDate, startDate, endDate))
                return iterationType.result();
        }
        return Collections.emptyList();
    }

    private String extract(String date, JsonObject json) {
        return json.get(date).getAsString().substring(0, 10);
    }

}

