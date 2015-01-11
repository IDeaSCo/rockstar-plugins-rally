package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static com.ideas.rally.SQLExecutor.executeUpdate;

public class EmailCallBack extends SFDCCallBack{
    @Override
    public List<String>  processResult(JsonArray jsonArray, String... input) throws Exception {
        List<String> output = new ArrayList<String>();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject json = jsonElement.getAsJsonObject();
            String email = json.get("EmailAddress").getAsString();
            output.add(email);
            updateOrInsertUserEmail(input[0], email);
        }
        return output;
    }

    private void updateOrInsertUserEmail(String owner, String email) throws Exception {
        if(executeUpdate("update user set email='"+email+"' where userName='"+owner+"'")[0] == 0) {
            executeUpdate("insert into user(userName,email) values('" + owner + "','" + email + "')");
        }
    }

    public String getUserEmailAddress(String owner) throws Exception {
        String email = getEmailFromDB(owner);
        if(email == null) {
            Fetch fetch = new Fetch("EmailAddress");
            QueryFilter queryFilter = new QueryFilter("DisplayName", "=", owner);
            SFDCExecutor executor = new SFDCExecutor("User", fetch, queryFilter, new EmailCallBack() , owner);
            return executor.execute().get(0);
        }
        return email;
    }

    private String getEmailFromDB(String owner) throws Exception {
        SQLExecutor SQLExecutor = new SQLExecutor("select email from user where userName='" + owner + "'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                this.result = rs.getString(1);
            }
        };
        SQLExecutor.go();
        return SQLExecutor.result;
    }




}

