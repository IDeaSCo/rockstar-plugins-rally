package com.ideas.rally;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static com.ideas.rally.SQLExecutor.executeUpdate;

public class TaskCallBackTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        RallyConfiguration.testRun=true;
        RallyConfiguration.createSchema();
    }

    @Before
    public void before() throws Exception{
        RallyConfiguration.getConnection().createStatement().execute("delete from taskHistory");
        RallyConfiguration.getConnection().createStatement().execute("delete from user");
        RallyConfiguration.getConnection().createStatement().execute("insert into user(userName,email) values('owner','email@email.com')");
    }

    @Test
    public void processResult_insert() throws Exception {
        TaskCallBack callBack = new TaskCallBack();
        List<String> input = new ArrayList<String>();
        input.add("Iteration 1");
        List<String> output = new ArrayList<String>();
        callBack.processResult(getTaskArray(), input, output);

        ResultSet rs = RallyConfiguration.getConnection().createStatement().executeQuery("select iteration,taskNumber,taskOwner,actuals,toDo, state, taskChanged from taskHistory");
        rs.next();
        Assert.assertEquals("Iteration 1",rs.getString(1));
        Assert.assertEquals("TA007",rs.getString(2));
        Assert.assertEquals("email@email.com",rs.getString(3));
        Assert.assertEquals("10.3",rs.getString(4));
        Assert.assertEquals("5.1",rs.getString(5));
        Assert.assertEquals("Defined",rs.getString(6));
        Assert.assertEquals("1",rs.getString(7));

    }

    @Test
         public void processResult_update_task_not_changed() throws Exception {
//Given:
        StringBuilder buffer = new StringBuilder()
                .append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ")
                .append(" values ('Iteration 1','TA007','email@email.com',10.3,5.1,'Defined',1) ");

        executeUpdate(buffer.toString());

//When:
        TaskCallBack callBack = new TaskCallBack();
        List<String> input = new ArrayList<String>();
        input.add("Iteration 1");
        List<String> output = new ArrayList<String>();
        callBack.processResult(getTaskArray(), input, output);

//Then:
        ResultSet rs = RallyConfiguration.getConnection().createStatement().executeQuery("select iteration,taskNumber,taskOwner,actuals,toDo, state, taskChanged from taskHistory");
        rs.next();
        Assert.assertEquals("Iteration 1",rs.getString(1));
        Assert.assertEquals("TA007",rs.getString(2));
        Assert.assertEquals("email@email.com",rs.getString(3));
        Assert.assertEquals("10.3",rs.getString(4));
        Assert.assertEquals("5.1",rs.getString(5));
        Assert.assertEquals("Defined",rs.getString(6));
        Assert.assertEquals("0",rs.getString(7));
    }


    @Test
    public void processResult_update_task_changed_because_of_change_in_actual() throws Exception {
//Given:
        StringBuilder buffer = new StringBuilder()
                .append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ")
                .append(" values ('Iteration 1','TA007','email@email.com',10.6,5.1,'Defined',1) ");

        executeUpdate(buffer.toString());

//When:
        TaskCallBack callBack = new TaskCallBack();
        List<String> input = new ArrayList<String>();
        input.add("Iteration 1");
        List<String> output = new ArrayList<String>();
        callBack.processResult(getTaskArray(), input, output);

//Then:
        ResultSet rs = RallyConfiguration.getConnection().createStatement().executeQuery("select iteration,taskNumber,taskOwner,actuals,toDo, state, taskChanged from taskHistory");
        rs.next();
        Assert.assertEquals("Iteration 1",rs.getString(1));
        Assert.assertEquals("TA007",rs.getString(2));
        Assert.assertEquals("email@email.com",rs.getString(3));
        Assert.assertEquals("10.3",rs.getString(4));
        Assert.assertEquals("5.1",rs.getString(5));
        Assert.assertEquals("Defined",rs.getString(6));
        Assert.assertEquals("1",rs.getString(7));
    }

    @Test
    public void processResult_update_task_changed_because_of_change_in_todo() throws Exception {
//Given:
        StringBuilder buffer = new StringBuilder()
                .append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ")
                .append(" values ('Iteration 1','TA007','email@email.com',10.3,4.1,'Defined',1) ");

        executeUpdate(buffer.toString());
//When:
        TaskCallBack callBack = new TaskCallBack();
        List<String> input = new ArrayList<String>();
        input.add("Iteration 1");
        List<String> output = new ArrayList<String>();
        callBack.processResult(getTaskArray(), input, output);

//Then:
        ResultSet rs = RallyConfiguration.getConnection().createStatement().executeQuery("select iteration,taskNumber,taskOwner,actuals,toDo, state, taskChanged from taskHistory");
        rs.next();
        Assert.assertEquals("Iteration 1",rs.getString(1));
        Assert.assertEquals("TA007",rs.getString(2));
        Assert.assertEquals("email@email.com",rs.getString(3));
        Assert.assertEquals("10.3",rs.getString(4));
        Assert.assertEquals("5.1",rs.getString(5));
        Assert.assertEquals("Defined",rs.getString(6));
        Assert.assertEquals("1",rs.getString(7));
    }
    @Test
    public void processResult_update_task_changed_because_of_change_in_state() throws Exception {
//Given:
        StringBuilder buffer = new StringBuilder()
                .append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ")
                .append(" values ('Iteration 1','TA007','email@email.com',10.3,5.1,'In-Progress',1) ");

        executeUpdate(buffer.toString());


//When:
        TaskCallBack callBack = new TaskCallBack();
        List<String> input = new ArrayList<String>();
        input.add("Iteration 1");
        List<String> output = new ArrayList<String>();
        callBack.processResult(getTaskArray(), input, output);

//Then:
        ResultSet rs = RallyConfiguration.getConnection().createStatement().executeQuery("select iteration,taskNumber,taskOwner,actuals,toDo, state, taskChanged from taskHistory");
        rs.next();
        Assert.assertEquals("Iteration 1",rs.getString(1));
        Assert.assertEquals("TA007",rs.getString(2));
        Assert.assertEquals("email@email.com",rs.getString(3));
        Assert.assertEquals("10.3",rs.getString(4));
        Assert.assertEquals("5.1",rs.getString(5));
        Assert.assertEquals("Defined",rs.getString(6));
        Assert.assertEquals("1",rs.getString(7));
    }
    private JsonArray getTaskArray(){
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getTask("owner","TA007","10.3","5.1","Defined"));
        return jsonArray;
    }

    private JsonElement getTask(String owner, String formattedID, String actuals, String toDo, String state)
    {
        String jsonStr =
                "{"+
                        " \"Owner\":{ \"_refObjectName\":\""+owner+"\"}, "+
                        " \"FormattedID\":\""+formattedID+"\", "+
                        " \"Actuals\":\""+actuals+"\", "+
                        " \"ToDo\":\""+toDo+"\", "+
                        " \"State\":\""+state+"\" "+
                 "}";
        Gson gson = new Gson();
        return gson.fromJson (jsonStr, JsonElement.class);
    }
}
