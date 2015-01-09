package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by idnasr on 1/8/2015.
 */
public class PreviousIterationCallBack extends SFDCCallBack{
    @Override
    public void procesResult(JsonArray jsonArray, List<String> input, List<String> output) throws Exception {
        List<String> iterationList = new ArrayList<String>();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject json = jsonElement.getAsJsonObject();
            String iterationName = json.get("Name").getAsString();
            String startDate = json.get("StartDate").getAsString().substring(0, 10);
            String endDate = json.get("EndDate").getAsString().substring(0, 10);
            System.out.println("iterationName:" + iterationName + " startDate:" + startDate + " endDate:" + endDate);

            if (startDate.compareTo(input.get(0)) <= 0 && endDate.compareTo(input.get(0)) >= 0) {
                if(iterationList.size() > 0 ) {
                    output.add(iterationList.get(iterationList.size() - 1));
                }
                break;
            }
            iterationList.add(iterationName);
        }
    }

}

