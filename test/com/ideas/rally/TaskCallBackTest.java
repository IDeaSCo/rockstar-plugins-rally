package com.ideas.rally;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.ResultSet;
import java.util.List;

import static com.ideas.rally.SQLExecutor.executeUpdate;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class TaskCallBackTest {
    private static final JsonArray taskArray = getTaskArray();
    private static final String iterationName = "Iteration 1";
    private static final String id = "TA007";
    private static final String email = "email@email.com";
    private static final String actuals = "10.3";
    private static final String toDo = "5.1";
    private static final String definedStatus = "Defined";
    private final TaskCallBack callBack = new TaskCallBack();

    @BeforeClass
    public static void setUpDB() throws Exception {
        RallyConfiguration.testRun = true;
        RallyConfiguration.createSchema();
        executeUpdate("insert into user(userName,email) values('owner','" + email + "')");
    }

    @Before
    public void seedData() throws Exception {
        executeUpdate("delete from taskHistory");
    }

    @Test
    public void newTasksHaveChangedStatus() throws Exception {
        callBack.processResult(taskArray, iterationName);
        assertTaskChangedStatusIs("1");
    }

    @Test
    public void tasksUpdatedWithSameDataDoNotHaveChangedStatus() throws Exception {
        insetIntoTaskHistory(actuals, toDo, definedStatus);
        callBack.processResult(taskArray, iterationName);
        assertTaskChangedStatusIs("0");
    }


    @Test
    public void whenActualsAreUpdatedOnTheTaskItHasChangedStatus() throws Exception {
        insetIntoTaskHistory("10.6", toDo, definedStatus);
        callBack.processResult(taskArray, iterationName);
        assertTaskChangedStatusIs("1");
    }

    @Test
    public void whenToDoIsUpdatedOnTheTaskItHasChangedStatus() throws Exception {
        insetIntoTaskHistory(actuals, "4.1", definedStatus);
        callBack.processResult(taskArray, iterationName);
        assertTaskChangedStatusIs("1");
    }

    @Test
    public void whenStateIsUpdatedOnTheTaskItHasChangedStatus() throws Exception {
        insetIntoTaskHistory(actuals, toDo, "In-Progress");
        callBack.processResult(taskArray, iterationName);
        assertTaskChangedStatusIs("1");
    }

    private static JsonArray getTaskArray() {
        JsonArray jsonArray = new JsonArray();
        jsonArray.add(getTask("owner", id, actuals, toDo, definedStatus));
        return jsonArray;
    }

    private static JsonElement getTask(String owner, String formattedID, String actuals, String toDo, String state) {
        String jsonStr =
                "{" +
                        " \"Owner\":{ \"_refObjectName\":\"" + owner + "\"}, " +
                        " \"FormattedID\":\"" + formattedID + "\", " +
                        " \"Actuals\":\"" + actuals + "\", " +
                        " \"ToDo\":\"" + toDo + "\", " +
                        " \"State\":\"" + state + "\" " +
                        "}";
        return new Gson().fromJson(jsonStr, JsonElement.class);
    }

    private void assertTaskChangedStatusIs(final String taskChangedStatus) throws Exception {
        new SQLExecutor("select iteration,taskNumber,taskOwner,actuals,toDo, state, taskChanged from taskHistory"){
            @Override
            public void accept(ResultSet rs) throws Exception {
                List<String> expected = asList(iterationName, id, email, actuals, toDo, definedStatus, taskChangedStatus);
                for (int i = 0; i < expected.size(); i++) {
                    assertEquals(expected.get(i), rs.getString(i+1));
                }
            }
        }.go();
    }

    private void insetIntoTaskHistory(String actuals, String toDo, String definedStatus) throws Exception {
        executeUpdate("insert into taskHistory values ('" + iterationName + "','" + id + "','" + email + "'," + actuals + "," + toDo + ",'" + definedStatus + "',1)");
    }
}
