package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;

import static com.ideas.rally.SQLExecutor.executeUpdate;

public class StoryIterationCallBack extends CallBack {
    @Override
    public List<String> processResult(JsonArray jsonArray, String... input) throws Exception {
        for (JsonElement jsonElement : jsonArray) {
            JsonObject json = jsonElement.getAsJsonObject();
            System.out.println("storyNumber:'" + input[0] + getReferenceName(json, "Iteration") + "'");
            if (getReferenceName(json, "Iteration") == null || !getReferenceName(json, "Iteration").equals(input[1])) {
                System.out.println("updating iteration name..");
                updateStoryIterationNumber(input[0], getReferenceName(json, "Iteration"));
            }
        }
        return Collections.emptyList();
    }

    private void updateStoryIterationNumber(String storyNumber, String iterationName) throws Exception {
        executeUpdate("insert into storyhistory(storyNumber,iteration) values ('" + storyNumber + "','" + iterationName + "') on duplicate key update iterationChanged=if(VALUES(iteration) = 'null',0,if(VALUES(iteration) <> iteration,1,0)), iteration=VALUES(iteration)");
    }
}
