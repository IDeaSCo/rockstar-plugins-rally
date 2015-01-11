package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ideas.rally.SQLExecutor.executeUpdate;

public class TaskCallBack extends SFDCCallBack{
    @Override
    public List<String> processResult(JsonArray jsonArray, List<String> input) throws Exception {
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
        return Collections.emptyList();
    }

    private void insertIntoTaskHistory(String iteration, String taskNumber, String taskOwner, float actual, float todo, String state) throws Exception {
        Map<String,String> input = new HashMap<String, String>();
        input.put("actuals", String.valueOf(actual));
        input.put("toDo",String.valueOf(todo));
        input.put("state",""+state);

        StringBuilder buffer = new StringBuilder()
        .append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ")
        .append(" values ('" + iteration + "','" + taskNumber + "','" + taskOwner + "'," + actual + "," + todo + ",'" + state + "',1) ");

        try {
            executeUpdate(buffer.toString());
        }catch(SQLException e){
            updateTaskHistory(iteration, taskNumber, taskOwner, input);
        }

    }

    private void updateTaskHistory(final String iteration, final String taskNumber, final String taskOwner, final Map<String, String> input) throws Exception {
        String query="select iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged from taskHistory where iteration='" + iteration + "' and taskNumber='" + taskNumber + "' and taskOwner='" + taskOwner + "'";

        SQLExecutor SQLExecutor = new SQLExecutor(query) {
            @Override
            public void accept(ResultSet rs) throws Exception {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append("update taskHistory ");
                    buffer.append("set ");
                    buffer.append("taskChanged= " + hasTaskStatusChanged(rs.getString("actuals"), rs.getString("toDo"), rs.getString("state"), input));
                    buffer.append(",actuals= " + input.get("actuals"));
                    buffer.append(",toDo= " + input.get("toDo"));
                    buffer.append(",state= '" + input.get("state") + "'");
                    buffer.append("where iteration='" + rs.getString("iteration") + "' and taskNumber='" + rs.getString("taskNumber") + "' and taskOwner='" + rs.getString("taskOwner") + "'");
                    executeUpdate(buffer.toString());
            }
        };
        SQLExecutor.go();
    }

    private int hasTaskStatusChanged(String actuals, String toDo, String state, Map<String, String> input) {
        if(!actuals.equals(input.get("actuals")))
            return 1;
        if(!toDo.equals(input.get("toDo")))
            return 1;
        if(!state.equals(input.get("state")))
            return 1;
        return 0;
    }

}

