package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

import static com.ideas.rally.SQLExecutor.executeUpdate;

/**
 * Created by idnasr on 1/8/2015.
 */
public class TaskCallBack extends SFDCCallBack{
    @Override
    public void procesResult(JsonArray jsonArray, List<String> input, List<String> output) throws Exception {
        for (JsonElement jsonElement : jsonArray) {
            JsonObject json = jsonElement.getAsJsonObject();
            String emailAddress = new EmailCallBack().getUserEmailAddress(getReferenceName(json, "Owner"));

            System.out.println(json.get("FormattedID") + ":" + json.get("Actuals") + ":" + json.get("ToDo") + ":" + json.get("State") + ":" + emailAddress);

            float actual = getFloatValue(json, "Actuals");
            float todo = getFloatValue(json, "ToDo");
            if (emailAddress != null) {
                insertIntoTaskHistory(input.get(0), json.get("FormattedID").getAsString(), emailAddress, actual, todo, json.get("State").getAsString());
            }
        }
    }



    private void insertIntoTaskHistory(String iteration, String taskNumber, String taskOwner, float actual, float todo, String state) throws Exception {
        StringBuilder buffer = new StringBuilder()
                .append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ")
                .append(" values ('" + iteration + "','" + taskNumber + "','" + taskOwner + "'," + actual + "," + todo + ",'" + state + "',1) ")
                .append(" on duplicate key update ")
                .append(" taskChanged=if(actuals<>VALUES(actuals),1,if(toDo<>VALUES(toDo),1,if(state<>VALUES(state),1,0))), ")
                .append(" actuals=VALUES(actuals), ")
                .append(" toDo=VALUES(toDo), ")
                .append(" state=VALUES(state) ");
        executeUpdate(buffer.toString());
    }

}

