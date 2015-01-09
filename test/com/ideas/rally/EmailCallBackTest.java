package com.ideas.rally;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by idnasr on 1/9/2015.
 */
public class EmailCallBackTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        RallyConfiguration.testRun=true;
        RallyConfiguration.createSchema();
    }

    @Before
    public void before() throws Exception{
        RallyConfiguration.getConnection().createStatement().execute("delete from user");
    }
    @Test
    public void processResult() throws Exception {
        String userName="testName";
        EmailCallBack  callBack = new EmailCallBack();
        List<String> input = new ArrayList<String>();
        input.add(userName);
        List<String> output = new ArrayList<String>();
        callBack.procesResult(getIterationArray("user.name@company.com"),input,output);

        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs =  stmt.executeQuery("select userName,email from user where userName='" + userName + "'");
        rs.next();
        Assert.assertEquals(":" + userName + ":", ":" + rs.getString(1) + ":");
        Assert.assertEquals("user.name@company.com",rs.getString(2));

        callBack.procesResult(getIterationArray("user.name@change.com"),input,output);

        rs = stmt.executeQuery("select userName,email from user where userName='" + userName + "'");
        rs.next();
        Assert.assertEquals(":" + userName + ":", ":" + rs.getString(1) + ":");
        Assert.assertEquals("user.name@change.com",rs.getString(2));
    }

    @Test
    public void getUserEmailAddress() throws Exception {
        RallyConfiguration.getConnection().createStatement().execute("insert into user(userName,email) values('owner','email@email.com')");
        EmailCallBack  callBack = new EmailCallBack();
        Assert.assertEquals("email@email.com",callBack.getUserEmailAddress("owner"));
    }
    private JsonArray getIterationArray(String email){
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getEmail(email));
        return jsonArray;
    }

    private JsonElement getEmail(String email)
    {
        String jsonStr =
                "{"+
                    " \"EmailAddress\":\""+email+"\" "+
                "}";
        Gson gson = new Gson();
        return gson.fromJson (jsonStr, JsonElement.class);
    }
}
