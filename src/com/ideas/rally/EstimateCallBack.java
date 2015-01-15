package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.response.GetResponse;

import java.util.Collections;
import java.util.List;

import static com.ideas.rally.SQLExecutor.executeUpdate;

public class EstimateCallBack extends CallBack {
    @Override
    public List<String> processResult(JsonArray jsonArray, String... input) throws Exception {
        for (JsonElement jsonElement : jsonArray) {
            JsonObject json = jsonElement.getAsJsonObject();
            String emailAddress = new EmailCallBack().getUserEmailAddress(getReferenceName(json, "Owner"));

            float planEstimate = getFloatValue(json, "PlanEstimate");
            if (emailAddress != null) {
                insertIntoStoryHistory(input[0], json.get("FormattedID").getAsString(), emailAddress, planEstimate, json.get("ScheduleState").getAsString());
                deleteStoryTaskOwners(json.get("FormattedID").getAsString());
                insertStoryTaskOwners(json.get("FormattedID").getAsString(), json.getAsJsonObject("Tasks").get("_ref").getAsString());
            }
            System.out.println(emailAddress + ":" + json.getAsJsonObject("Tasks").get("_ref").getAsString());
        }
        return Collections.emptyList();
    }

    private void insertIntoStoryHistory(String iteration, String storyNumber, String emailAddress, float planEstimate, String state) throws Exception {
        StringBuilder buffer = new StringBuilder()
                .append(" insert into storyHistory(iteration,storyNumber,storyOwner,planEstimate,state,planEstimateChanged,stateChanged,iterationChanged) ")
                .append(" values ('" + iteration + "','" + storyNumber + "','" + emailAddress + "'," + planEstimate + ",'" + state + "',if(" + planEstimate + " <> 0,1,0),if('" + state + "' = 'Completed',1,if('" + state + "' = 'Accepted',2,0)),0) ")
                .append(" on duplicate key update ")
                .append(" iterationChanged=if(iteration = 'null',0,if(VALUES(iteration) <> iteration,1,0)), ")
                .append(" planEstimateChanged=if(VALUES(planEstimate) > 0,if(planEstimate=0,1,0),0), ")
                .append(" stateChanged=if(state<>VALUES(state),if(VALUES(state) = 'Accepted', if(state<>'Completed',2,1),1),0), ")
                .append(" iteration=VALUES(iteration), ")
                .append(" storyOwner=VALUES(storyOwner), ")
                .append(" planEstimate=VALUES(planEstimate), ")
                .append(" state=VALUES(state) ");
        executeUpdate(buffer.toString());
    }

    private void deleteStoryTaskOwners(String storyNumber) throws Exception {
        executeUpdate("delete from storyUsers where storyNumber='" + storyNumber + "'");
    }

    private void insertStoryTaskOwners(String storyNumber, String taskURL) throws Exception {
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();
        GetRequest request = new GetRequest(taskURL);
        GetResponse response = restApi.get(request);
        JsonObject json = response.getObject();
        JsonArray jsonArray = json.getAsJsonArray("Results");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            JsonObject childObject = element.getAsJsonObject();
            if (!(childObject.get("Owner") instanceof JsonNull)) {
                String email = new EmailCallBack().getUserEmailAddress(childObject.getAsJsonObject("Owner").get("_refObjectName").getAsString());
                if (email != null) {
                    insertIntoStoryUsers(storyNumber, email);
                }
            }
        }
    }

    private void insertIntoStoryUsers(String storyNumber, String emailAddress) throws Exception {
        executeUpdate("insert into storyUsers(storyNumber,storyTaskOwner) " + " values ('" + storyNumber + "','" + emailAddress + "') " + " on duplicate key update " + " storyTaskOwner=VALUES(storyTaskOwner) ");
    }
}
