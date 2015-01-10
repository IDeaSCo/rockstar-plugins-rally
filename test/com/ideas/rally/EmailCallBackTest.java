package com.ideas.rally;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static com.ideas.rally.SQLExecutor.executeUpdate;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class EmailCallBackTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        RallyConfiguration.testRun=true;
        RallyConfiguration.createSchema();
    }

    @Before
    public void before() throws Exception{
        executeUpdate("delete from user");
    }

    @Test
    public void processResult() throws Exception {
        final String userName="testName";
        final String email = "user.name@company.com";
        String updatedEmailed = "user.name@change.com";

        EmailCallBack  callBack = new EmailCallBack();
        List<String> input = asList(userName);
        List<String> output = new ArrayList<String>();

        callBack.processResult(asJsonArray(email), input, output);
        assertValuesInDBMatch(userName, email);

        callBack.processResult(asJsonArray(updatedEmailed), input, output);
        assertValuesInDBMatch(userName, updatedEmailed);
    }

    @Test
    public void getUserEmailAddress() throws Exception {
        String email = "email@email.com";
        executeUpdate("insert into user(userName,email) values('owner','" + email + "')");
        EmailCallBack  callBack = new EmailCallBack();
        assertEquals(email, callBack.getUserEmailAddress("owner"));
    }

    private void assertValuesInDBMatch(final String userName, final String email) throws Exception {
        String query = "select userName, email from user where userName='" + userName + "'";
        new SQLExecutor(query) {
            @Override
            public void accept(ResultSet rs) throws Exception {
                assertEquals(userName, rs.getString(1));
                assertEquals(email, rs.getString(2));
            }
        }.go();
    }

    private JsonArray asJsonArray(String email){
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getEmail(email));
        return jsonArray;
    }

    private JsonElement getEmail(String email)
    {
        String jsonStr = "{ \"EmailAddress\":\""+email+"\" }";
        Gson gson = new Gson();
        return gson.fromJson (jsonStr, JsonElement.class);
    }
}
