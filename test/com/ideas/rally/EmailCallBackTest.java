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
    private final EmailCallBack callBack = new EmailCallBack();
    private final String userName = "testName";
    private final String email = "user.name@company.com";
    private final List<String> userNames = asList(userName);
    private final List<String> emails = new ArrayList<String>();


    @BeforeClass
    public static void beforeClass() throws Exception {
        RallyConfiguration.testRun = true;
        RallyConfiguration.createSchema();
    }

    @Before
    public void before() throws Exception {
        executeUpdate("delete from user");
    }

    @Test
    public void insertNewUserIfItDoesNotExist() throws Exception {
        callBack.processResult(asJsonArray(email), userNames, emails);
        assertEquals(email, emails.get(0));
        assertValuesInDBMatch(userName, email);
    }

    @Test
    public void updateEmailIfUserAlreadyPresent() throws Exception {
        callBack.processResult(asJsonArray(email), userNames, emails);
        assertValuesInDBMatch(userName, email);

        String updatedEmailed = "user.name@change.com";
        callBack.processResult(asJsonArray(updatedEmailed), userNames, emails);
        assertValuesInDBMatch(userName, updatedEmailed);
    }

    @Test
    public void retrieveEmailUsingUserName() throws Exception {
        executeUpdate("insert into user(userName,email) values('" + userName + "','" + email + "')");
        assertEquals(email, callBack.getUserEmailAddress(userName));
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

    private JsonArray asJsonArray(String email) {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getEmail(email));
        return jsonArray;
    }

    private JsonElement getEmail(String email) {
        String jsonStr = "{ \"EmailAddress\":\"" + email + "\" }";
        Gson gson = new Gson();
        return gson.fromJson(jsonStr, JsonElement.class);
    }
}
