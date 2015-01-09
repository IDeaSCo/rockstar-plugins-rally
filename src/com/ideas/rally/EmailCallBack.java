package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ideas.rally.SQLExecutor.executeUpdate;

/**
 * Created by idnasr on 1/8/2015.
 */
public class EmailCallBack extends SFDCCallBack{
    @Override
    public void procesResult(JsonArray jsonArray, List<String> input, List<String> output) throws Exception {
        for (JsonElement jsonElement : jsonArray) {
            JsonObject json = jsonElement.getAsJsonObject();
            String email = json.get("EmailAddress").getAsString();
            output.add(email);
            updateEmail(input.get(0), email);
        }
    }

    private void updateEmail(String owner, String email) throws Exception {
        if(executeUpdate("update user set email='"+email+"' where userName='"+owner+"'")[0] == 0) {
            executeUpdate("insert into user(userName,email) values('" + owner + "','" + email + "')");
        }
    }

    public String getUserEmailAddress(String owner) throws Exception {
        String email = getEmailFromDB(owner);
        if(email == null) {
            Fetch fetch = new Fetch("EmailAddress");
            List<String> input = new ArrayList<String>();
            input.add(owner);
            List<String> output = new ArrayList<String>();
            QueryFilter queryFilter = new QueryFilter("DisplayName", "=", owner);
            SFDCExecutor executor = new SFDCExecutor("User", fetch, queryFilter, new EmailCallBack() , input, output);
            return output.get(0);
        }
        return email;
    }

    private String getEmailFromDB(String owner) throws Exception {
        SQLExecutor SQLExecutor = new SQLExecutor(null,"select email from user where userName='" + owner + "'") {
            @Override
            public void accept(ResultSet rs,Map<String,String>input) throws Exception {
                this.result = rs.getString(1);
            }
        };
        SQLExecutor.go();
        return SQLExecutor.result;
    }




}

