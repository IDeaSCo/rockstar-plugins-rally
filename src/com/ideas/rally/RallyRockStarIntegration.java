package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.GetRequest;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.GetResponse;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;

public class RallyRockStarIntegration {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final List<String> holidays = new ArrayList<String>();

    static {
        try {
            loadHolidays();
        } catch (Exception e) {
            System.err.println("Could not load holidays list.");
            e.printStackTrace();
        }
    }

    private static void loadHolidays() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader("holidays.list"));
        String line;
        while ((line = reader.readLine()) != null) {
            holidays.add(line);
        }
        reader.close();
    }

    private static final List<String> workingDaysSinceStartOfIteration = new ArrayList<String>();

    private static String getIteration(String date) throws Exception {

        String iterationName;
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();

        QueryRequest workSpaceRequest = new QueryRequest("Iteration");
        workSpaceRequest.setFetch(new Fetch("Name", "StartDate", "EndDate"));
        QueryFilter queryFilter = new QueryFilter("Project.Name", "=", RallyConfiguration.RALLY_PROJECT);
        workSpaceRequest.setQueryFilter(queryFilter);
        workSpaceRequest.setPageSize(200);
        int pageCount = 0;
        QueryResponse workSpaceResponse;
        do {
            workSpaceRequest.setLimit(1);
            workSpaceRequest.setStart((pageCount * 200) + pageCount);
            pageCount++;
            workSpaceResponse = restApi.query(workSpaceRequest);
            JsonArray array = workSpaceResponse.getResults();
            for (JsonElement jsonElement : array) {

                JsonObject json = jsonElement.getAsJsonObject();
                iterationName = json.get("Name").getAsString();
                String startDate = json.get("StartDate").getAsString().substring(0, 10);
                String endDate = json.get("EndDate").getAsString().substring(0, 10);
                endDate = subtractOneDay(endDate);
                System.out.println("iterationName:" + iterationName + " startDate:" + startDate + " endDate:" + endDate);
                if (startDate.compareTo(date) <= 0 && endDate.compareTo(date) >= 0) {
                    populateWorkingDaysSinceStartOfIteration(startDate, date);
                    return iterationName;
                }
            }

        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);

        return null;

    }

    private static String subtractOneDay(String date) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sdf.parse(date));
        calendar.add(Calendar.DATE, -1);
        return sdf.format(calendar.getTime());
    }

    protected static void populateWorkingDaysSinceStartOfIteration(String startDate, String endDate) throws Exception {
        Calendar startCalDate = Calendar.getInstance();
        startCalDate.setTime(sdf.parse(startDate));
        Calendar endCalDate = Calendar.getInstance();
        endCalDate.setTime(sdf.parse(endDate));
        while (!startCalDate.after(endCalDate)) {
            if (startCalDate.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && startCalDate.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && !holidays.contains(sdf.format(startCalDate.getTime()))) {
                workingDaysSinceStartOfIteration.add(sdf.format(startCalDate.getTime()));
            }
            startCalDate.add(Calendar.DATE, 1);
        }

    }

    private static String getPreviousIteration(String date) throws Exception {
        String iterationName;
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();

        QueryRequest workSpaceRequest = new QueryRequest("Iteration");
        workSpaceRequest.setFetch(new Fetch("Name", "StartDate", "EndDate"));
        QueryFilter queryFilter = new QueryFilter("Project.Name", "=", RallyConfiguration.RALLY_PROJECT);
        workSpaceRequest.setQueryFilter(queryFilter);
        workSpaceRequest.setPageSize(200);
        int pageCount = 0;
        QueryResponse workSpaceResponse;
        do {
            workSpaceRequest.setLimit(1);
            workSpaceRequest.setStart((pageCount * 200) + pageCount);
            pageCount++;
            workSpaceResponse = restApi.query(workSpaceRequest);
            JsonArray array = workSpaceResponse.getResults();
            for (JsonElement jsonElement : array) {

                JsonObject json = jsonElement.getAsJsonObject();
                iterationName = json.get("Name").getAsString();
                String startDate = json.get("StartDate").getAsString().substring(0, 10);
                String endDate = json.get("EndDate").getAsString().substring(0, 10);
                System.out.println("iterationName:" + iterationName + " startDate:" + startDate + " endDate:" + endDate);


                if (startDate.compareTo(date) <= 0 && endDate.compareTo(date) >= 0) {
                    Date dStartDate = sdf.parse(startDate);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dStartDate);
                    cal.add(Calendar.DATE, -1);
                    return getIteration(sdf.format(cal.getTime()));
                }
            }

        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);

        return null;

    }

    private static void getTasks(String iteration) throws Exception {
        System.out.println("iteration:" + iteration);
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();

        QueryRequest workSpaceRequest = new QueryRequest("Task");
        workSpaceRequest.setFetch(new Fetch("FormattedID", "Actuals", "Blocked", "State", "ToDo", "Owner"));
        QueryFilter queryFilter = new QueryFilter("Iteration.Name", "=", iteration);
        workSpaceRequest.setQueryFilter(queryFilter);
        workSpaceRequest.setPageSize(200);
        int pageCount = 0;
        QueryResponse workSpaceResponse;
        do {
            workSpaceRequest.setLimit(1);
            workSpaceRequest.setStart((pageCount * 200) + pageCount);
            pageCount++;
            workSpaceResponse = restApi.query(workSpaceRequest);
            System.out.println("Total Result:" + workSpaceResponse.getTotalResultCount());
            JsonArray array = workSpaceResponse.getResults();
            for (JsonElement jsonElement : array) {
                JsonObject json = jsonElement.getAsJsonObject();
                String emailAddress = getUserEmailAddress(getReferenceName(json, "Owner"));

                System.out.println(json.get("FormattedID") + ":" + json.get("Actuals") + ":" + json.get("ToDo") + ":" + json.get("State") + ":" + emailAddress);

                float actual = getFloatValue(json, "Actuals");
                float todo = getFloatValue(json, "ToDo");
                if (emailAddress != null) {
                    insertIntoTaskHistory(iteration, json.get("FormattedID").getAsString(), emailAddress, actual, todo, json.get("State").getAsString());
                }
            }

        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);

    }

    private static float getFloatValue(JsonObject json, String value) {
        if (json == null || json.get(value) instanceof JsonNull || json.get(value) == null) {
            return 0.0f;
        }
        return json.get(value).getAsFloat();
    }

    private static void getStories(String iteration, String storyDefect) throws Exception {
        getStoryDefect(storyDefect, "Iteration.Name", iteration, iteration);
        updateStoryIteration(iteration);
    }

    private static void getStoryDefect(String storyDefect, String filter, String filterValue, String iteration)
            throws Exception {
        System.out.println("iteration:" + iteration);
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();

        QueryRequest workSpaceRequest = new QueryRequest(storyDefect);
        workSpaceRequest.setFetch(new Fetch("FormattedID", "ScheduleState", "PlanEstimate", "Tasks", "Owner"));
        QueryFilter queryFilter = new QueryFilter(filter, "=", filterValue);
        workSpaceRequest.setQueryFilter(queryFilter);
        workSpaceRequest.setPageSize(200);
        int pageCount = 0;
        QueryResponse workSpaceResponse;
        do {
            workSpaceRequest.setLimit(1);
            workSpaceRequest.setStart((pageCount * 200) + pageCount);
            pageCount++;
            workSpaceResponse = restApi.query(workSpaceRequest);
            System.out.println("Total Result:" + workSpaceResponse.getTotalResultCount());
            JsonArray array = workSpaceResponse.getResults();
            System.out.println("================");
            for (JsonElement jsonElement : array) {

                JsonObject json = jsonElement.getAsJsonObject();
                String emailAddress = getUserEmailAddress(getReferenceName(json, "Owner"));

                float planEstimate = getFloatValue(json, "PlanEstimate");
                if (emailAddress != null) {
                    insertIntoStoryHistory(iteration, json.get("FormattedID").getAsString(), emailAddress, planEstimate, json.get("ScheduleState").getAsString());
                    deleteStoryTaskOwners(json.get("FormattedID").getAsString());
                    insertStoryTaskOwners(json.get("FormattedID").getAsString(), json.getAsJsonObject("Tasks").get("_ref").getAsString());
                }
                System.out.println(emailAddress + ":" + json.getAsJsonObject("Tasks").get("_ref").getAsString());

            }
            System.out.println("================");
        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);


    }

    private static void getStoryIteration(String storyDefect, String storyNumber, String expectedIteration) throws Exception {
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();
        QueryRequest workSpaceRequest = new QueryRequest(storyDefect);
        workSpaceRequest.setFetch(new Fetch("Iteration"));
        QueryFilter queryFilter = new QueryFilter("FormattedID", "=", storyNumber);
        workSpaceRequest.setQueryFilter(queryFilter);
        workSpaceRequest.setPageSize(200);
        int pageCount = 0;
        QueryResponse workSpaceResponse;
        do {
            workSpaceRequest.setLimit(1);
            workSpaceRequest.setStart((pageCount * 200) + pageCount);
            pageCount++;
            workSpaceResponse = restApi.query(workSpaceRequest);
            System.out.println("Total Result:" + workSpaceResponse.getTotalResultCount());
            JsonArray array = workSpaceResponse.getResults();
            System.out.println("================");
            for (JsonElement jsonElement : array) {

                JsonObject json = jsonElement.getAsJsonObject();
                System.out.println("storyNumber:'" + storyNumber + getReferenceName(json, "Iteration") + "'");
                if (getReferenceName(json, "Iteration") == null || !getReferenceName(json, "Iteration").equals(expectedIteration)) {
                    System.out.println("updating iteration name..");
                    updateStoryIterationNumber(storyNumber, getReferenceName(json, "Iteration"));
                }
            }
            System.out.println("================");
        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);
    }

    private static void updateStoryIterationNumber(String storyNumber, String iterationName) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        stmt.execute("insert into storyhistory(storyNumber,iteration) values ('" + storyNumber + "','" + iterationName + "') on duplicate key update iterationChanged=if(VALUES(iteration) = 'null',0,if(VALUES(iteration) <> iteration,1,0)), iteration=VALUES(iteration)");
        stmt.close();
        con.close();
    }

    private static void updateStoryIteration(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select storyNumber from storyhistory where iteration='" + iteration + "'");
        while (rs.next()) {
            if (rs.getString(1).startsWith("US"))
                getStoryIteration("HierarchicalRequirement", rs.getString(1), iteration);
            else
                getStoryIteration("Defect", rs.getString(1), iteration);
        }
        stmt.close();
        con.close();
    }

    private static void deleteStoryTaskOwners(String storyNumber) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        stmt.execute("delete from storyUsers where storyNumber='" + storyNumber + "'");
        stmt.close();
        con.close();
    }

    private static void insertStoryTaskOwners(String storyNumber, String taskURL) throws Exception {
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();
        GetRequest request = new GetRequest(taskURL);
        GetResponse response = restApi.get(request);
        JsonObject json = response.getObject();
        JsonArray jsonArray = json.getAsJsonArray("Results");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonElement element = jsonArray.get(i);
            JsonObject childObject = element.getAsJsonObject();
            if (!(childObject.get("Owner") instanceof JsonNull)) {
                String email = getUserEmailAddress(childObject.getAsJsonObject("Owner").get("_refObjectName").getAsString());
                if (email != null) {
                    insertIntoStoryUsers(storyNumber, email);
                }
            }
        }
    }

    private static void insertIntoStoryHistory(String iteration, String storyNumber, String emailAddress, float planEstimate, String state) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append(" insert into storyHistory(iteration,storyNumber,storyOwner,planEstimate,state,planEstimateChanged,stateChanged,iterationChanged) ");
        buffer.append(" values ('" + iteration + "','" + storyNumber + "','" + emailAddress + "'," + planEstimate + ",'" + state + "',if(" + planEstimate + " <> 0,1,0),if('" + state + "' = 'Completed',1,if('" + state + "' = 'Accepted',2,0)),0) ");
        buffer.append(" on duplicate key update ");

        buffer.append(" iterationChanged=if(iteration = 'null',0,if(VALUES(iteration) <> iteration,1,0)), ");
        buffer.append(" planEstimateChanged=if(VALUES(planEstimate) > 0,if(planEstimate=0,1,0),0), ");
        buffer.append(" stateChanged=if(state<>VALUES(state),if(VALUES(state) = 'Accepted', if(state<>'Completed',2,1),1),0), ");
        buffer.append(" iteration=VALUES(iteration), ");
        buffer.append(" storyOwner=VALUES(storyOwner), ");
        buffer.append(" planEstimate=VALUES(planEstimate), ");
        buffer.append(" state=VALUES(state) ");
        stmt.execute(buffer.toString());
        stmt.close();
        con.close();
    }

    private static void insertIntoStoryUsers(String storyNumber, String emailAddress) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append(" insert into storyUsers(storyNumber,storyTaskOwner) ");
        buffer.append(" values ('" + storyNumber + "','" + emailAddress + "') ");
        buffer.append(" on duplicate key update ");
        buffer.append(" storyTaskOwner=VALUES(storyTaskOwner) ");
        stmt.execute(buffer.toString());
        stmt.close();
        con.close();
    }

    private static String getUserEmailAddress(String owner) throws Exception {
        if (owner == null) return null;

        String email = getEmailFromDB(owner);
        if (email == null) {
            RallyRestApi restApi = RallyConfiguration.getRallyRestApi();
            QueryRequest workSpaceRequest = new QueryRequest("User");
            workSpaceRequest.setFetch(new Fetch("EmailAddress"));
            QueryFilter queryFilter = new QueryFilter("DisplayName", "=", owner);
            workSpaceRequest.setQueryFilter(queryFilter);
            workSpaceRequest.setPageSize(200);
            int pageCount = 0;
            QueryResponse workSpaceResponse;
            do {
                workSpaceRequest.setLimit(1);
                workSpaceRequest.setStart((pageCount * 200) + pageCount);
                pageCount++;
                workSpaceResponse = restApi.query(workSpaceRequest);
                System.out.println("Total Result:" + workSpaceResponse.getTotalResultCount());
                JsonArray array = workSpaceResponse.getResults();
                for (JsonElement jsonElement : array) {
                    JsonObject json = jsonElement.getAsJsonObject();
                    email = json.get("EmailAddress").getAsString();
                    updateEmail(owner, email);
                }

            } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);
        }
        return email;
    }

    private static void updateEmail(String owner, String email) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        stmt.execute("insert ignore into user(userName,email) values('" + owner + "','" + email + "')");
        stmt.close();
        con.close();
    }

    private static String getEmailFromDB(String owner) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select email from user where userName='" + owner + "'");
        String email = null;
        if (rs.next()) {
            email = rs.getString(1);
        }
        rs.close();
        stmt.close();
        con.close();
        return email;
    }

    private static String getReferenceName(JsonObject json, String name) {
        if (json.get(name) instanceof JsonNull)
            return null;
        JsonObject owner = json.get(name).getAsJsonObject();
        return owner.get("_refObjectName").getAsString();
    }

    private static void insertIntoTaskHistory(String iteration, String taskNumber, String taskOwner, float actual, float todo, String state) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ");
        buffer.append(" values ('" + iteration + "','" + taskNumber + "','" + taskOwner + "'," + actual + "," + todo + ",'" + state + "',1) ");
        buffer.append(" on duplicate key update ");
        buffer.append(" taskChanged=if(actuals<>VALUES(actuals),1,if(toDo<>VALUES(toDo),1,if(state<>VALUES(state),1,0))), ");
        buffer.append(" actuals=VALUES(actuals), ");
        buffer.append(" toDo=VALUES(toDo), ");
        buffer.append(" state=VALUES(state) ");
        stmt.execute(buffer.toString());
        stmt.close();
        con.close();
    }

    private static void createSchema() throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder query = new StringBuilder();
        query.append(" CREATE TABLE if not exists `storyhistory` ( ");
        query.append("   `iteration` char(20) DEFAULT NULL, ");
        query.append("   `storyNumber` char(10) NOT NULL DEFAULT '', ");
        query.append("   `storyOwner` char(80) DEFAULT NULL, ");
        query.append("   `planEstimate` float DEFAULT NULL, ");
        query.append("   `state` char(12) DEFAULT NULL, ");
        query.append("   `planEstimateChanged` tinyint(1) DEFAULT NULL, ");
        query.append("   `stateChanged` tinyint(1) DEFAULT NULL, ");
        query.append("   `iterationChanged` tinyint(1) DEFAULT NULL, ");
        query.append("   `spillover` int(11) DEFAULT '0', ");
        query.append("   PRIMARY KEY (`storyNumber`) ");
        query.append(" ) ENGINE=MyISAM DEFAULT CHARSET=utf8; ");

        stmt.execute(query.toString());
        query = new StringBuilder();

        query.append(" CREATE TABLE if not exists `storyusers` ( ");
        query.append("   `storyNumber` char(10) NOT NULL DEFAULT '', ");
        query.append("   `storyTaskOwner` char(80) NOT NULL DEFAULT '', ");
        query.append("   PRIMARY KEY (`storyNumber`,`storyTaskOwner`) ");
        query.append(" ) ENGINE=MyISAM DEFAULT CHARSET=utf8; ");

        stmt.execute(query.toString());
        query = new StringBuilder();

        query.append(" CREATE TABLE if not exists `taskhistory` ( ");
        query.append("   `iteration` char(20) NOT NULL DEFAULT '', ");
        query.append("   `taskNumber` char(10) NOT NULL DEFAULT '', ");
        query.append("   `taskOwner` char(80) NOT NULL DEFAULT '', ");
        query.append("   `actuals` float DEFAULT NULL, ");
        query.append("   `toDo` float DEFAULT NULL, ");
        query.append("   `state` char(12) DEFAULT NULL, ");
        query.append("   `taskChanged` tinyint(1) DEFAULT NULL, ");
        query.append("   PRIMARY KEY (`iteration`,`taskNumber`,`taskOwner`) ");
        query.append(" ) ENGINE=MyISAM DEFAULT CHARSET=utf8; ");

        stmt.execute(query.toString());
        query = new StringBuilder();
        query.append(" CREATE TABLE if not exists `user` ( ");
        query.append("   `userName` char(90) NOT NULL DEFAULT '', ");
        query.append("   `email` char(90) DEFAULT NULL, ");
        query.append("   `leadAndAbove` tinyint(1) DEFAULT '0', ");
        query.append("   PRIMARY KEY (`userName`) ");
        query.append(" ) ENGINE=MyISAM DEFAULT CHARSET=utf8; ");

        stmt.execute(query.toString());

    }

    public static void main(String args[]) throws Exception {
        createSchema();
        if (!RallyRockStarIntegration.post(RallyConfiguration.postEmail, 0, "Test Connection", "Default")) return;

        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE, -1);
        String stringDate = sdf.format(date.getTime());

        if (date.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            return;
        }
        if (holidays.contains(stringDate)) {
            return;
        }

        String iteration = getIteration(stringDate);
        String previousIteration = getPreviousIteration(stringDate);
        System.out.println("Got iteration as:" + iteration + " for date:" + stringDate);
        if (iteration != null && !iteration.equals("null")) {
            getTasks(iteration);
            cleanUpDeletedTasks(iteration);

            getStories(iteration, "HierarchicalRequirement");
            getStories(iteration, "Defect");
            cleanUpDeletedStoriesDefects(iteration);
            updateStatusOfUnacceptedStoriesInPreviousIteration(previousIteration);

            optimizeTables();

            updateStarForNotUpdatingStoryPlannedEstimates(iteration);
            updateStarForUpdatingStoryPlannedEstimates(iteration);
            updateStarForSpillingOverStory();

            updateStarForGettingStoryAcceptedInIteration(iteration, stringDate);
            updateStarForCompletingStoryInIteration(iteration);
            updateStarForNotTaskingStory(iteration);

            updateStarForLeavingStoryInPriorIterationsWhichIsNotAccepted(previousIteration);
            updateStarForNotUpdatingRally(iteration);
            updateStarForUpdatingRally(iteration);

        }
    }

    private static void updateStatusOfUnacceptedStoriesInPreviousIteration(String previousIteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();

        buffer.append(" select storyNumber,storyHistory.iteration,state from storyHistory ");
        buffer.append(" where iteration='" + previousIteration + "' ");
        buffer.append(" and state<>'Accepted' ");

        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            String storyNumber = rs.getString(1);
            String storyType = storyNumber.startsWith("US") ? "HierarchicalRequirement" : "Defect";
            getStoryDefect(storyType, "FormattedID", storyNumber, previousIteration);
        }

        rs.close();
        stmt.close();
        con.close();
    }

    private static void optimizeTables() throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        stmt.execute("optimize table storyhistory");
        stmt.execute("optimize table storyusers");
        stmt.execute("optimize table taskhistory");
        stmt.execute("optimize table user");

        stmt.close();
        con.close();

    }

    private static void cleanUpDeletedStoriesDefects(String iteration) throws Exception {
        System.out.println("iteration:" + iteration);
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append("select storyNumber from storyhistory where iteration='" + iteration + "'");
        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            cleanUpStoriesIfRecycled(rs.getString(1));
        }
        stmt.close();
        con.close();
    }

    private static void cleanUpStoriesIfRecycled(String storyDefectNumber) throws Exception {
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();
        String storyDefect = storyDefectNumber.startsWith("DE") ? "Defect" : "HierarchicalRequirement";
        QueryRequest workSpaceRequest = new QueryRequest(storyDefect);
        workSpaceRequest.setFetch(new Fetch("Recycled", "DirectChildrenCount"));
        QueryFilter queryFilter = new QueryFilter("FormattedID", "=", storyDefectNumber);
        workSpaceRequest.setQueryFilter(queryFilter);
        workSpaceRequest.setPageSize(200);
        int pageCount = 0;
        QueryResponse workSpaceResponse;
        do {
            workSpaceRequest.setLimit(1);
            workSpaceRequest.setStart((pageCount * 200) + pageCount);
            pageCount++;
            workSpaceResponse = restApi.query(workSpaceRequest);
            System.out.println("Total Result:" + workSpaceResponse.getTotalResultCount());
            if (workSpaceResponse.getTotalResultCount() == 0) {
                deleteStoryNumber(storyDefectNumber);
            }
            JsonArray array = workSpaceResponse.getResults();
            for (JsonElement jsonElement : array) {
                JsonObject json = jsonElement.getAsJsonObject();
                if (json.get("Recycled").getAsBoolean()) {
                    deleteStoryNumber(storyDefectNumber);
                }
                if (json.get("DirectChildrenCount") != null) {
                    if (json.get("DirectChildrenCount").getAsInt() > 0) {
                        deleteStoryNumber(storyDefectNumber);
                    }
                }
            }
        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);
    }

    private static void cleanUpDeletedTasks(String iteration) throws Exception {
        System.out.println("iteration:" + iteration);
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();

        buffer.append("select taskNumber from taskHistory where iteration='" + iteration + "'");
        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            cleanUpTaskIfRecycled(rs.getString(1));
        }
        stmt.close();
        con.close();
    }

    private static void cleanUpTaskIfRecycled(String taskNumber)
            throws Exception {
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();

        QueryRequest workSpaceRequest = new QueryRequest("Task");
        workSpaceRequest.setFetch(new Fetch("Recycled"));
        QueryFilter queryFilter = new QueryFilter("FormattedID", "=", taskNumber);
        workSpaceRequest.setQueryFilter(queryFilter);
        workSpaceRequest.setPageSize(200);
        int pageCount = 0;
        QueryResponse workSpaceResponse;
        do {
            workSpaceRequest.setLimit(1);
            workSpaceRequest.setStart((pageCount * 200) + pageCount);
            pageCount++;
            workSpaceResponse = restApi.query(workSpaceRequest);
            System.out.println("Total Result:" + workSpaceResponse.getTotalResultCount());
            if (workSpaceResponse.getTotalResultCount() == 0) {
                deleteTaskNumber(taskNumber);
            }
            JsonArray array = workSpaceResponse.getResults();
            for (JsonElement jsonElement : array) {
                JsonObject json = jsonElement.getAsJsonObject();
                if (json.get("Recycled").getAsBoolean()) {
                    deleteTaskNumber(taskNumber);
                }
            }
        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);
    }

    private static void deleteTaskNumber(String taskNumber) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        stmt.execute("delete from taskHistory where taskNumber='" + taskNumber + "'");
        stmt.close();
        con.close();
    }

    private static void deleteStoryNumber(String storyNumber) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        stmt.execute("delete from storyhistory where storyNumber='" + storyNumber + "'");
        stmt.execute("delete from storyusers where storyNumber='" + storyNumber + "'");
        stmt.close();
        con.close();
    }

    private static void updateStarForLeavingStoryInPriorIterationsWhichIsNotAccepted(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();

        buffer.append(" select storyNumber,storyTaskOwner,storyHistory.iteration,state from storyHistory left join storyUsers using (storyNumber) ");
        buffer.append(" where iteration='" + iteration + "' ");
        buffer.append(" and state<>'Accepted' ");

        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), -5, "For leaving the story/defect :" + rs.getString(1) + " in iteration :" + rs.getString(3) + " in state " + rs.getString(4), "Process Violator");
        }

        rs.close();
        stmt.close();
        con.close();
    }

    private static void updateStarForNotTaskingStory(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();

        buffer.append(" select storyNumber,storyOwner,storyTaskOwner from storyHistory left join storyUsers using (storyNumber) ");
        buffer.append(" where iteration='" + iteration + "' ");
        buffer.append(" group by 1 ");
        buffer.append(" having storyTaskOwner is null ");


        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), -5, "For not tasking story:" + rs.getString(1), "Process Violator");
        }
        buffer = new StringBuilder();
        buffer.append("  select storyNumber,email from user cross join (select storyNumber from storyHistory left join storyUsers using (storyNumber) where iteration='" + iteration + "' and storyTaskOwner is null group by 1) as a where leadandabove=1");
        rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), -2, "For not tasking story:" + rs.getString(1), "Process Violator");
        }

        rs.close();
        stmt.close();
        con.close();
    }

    private static void updateStarForCompletingStoryInIteration(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append(" select distinct storyNumber,storyOwner from storyHistory where stateChanged=1 and state='Completed' and iteration='" + iteration + "' ");
        buffer.append("  union DISTINCT ");
        buffer.append("  select distinct storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where stateChanged=1 and state='Completed' and iteration='" + iteration + "') ");

        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), 10, "For completing the story:" + rs.getString(1), "Delivery Champ");
        }
        stmt.execute("update storyHistory set stateChanged=0 where stateChanged=1 and state='Completed' and iteration='" + iteration + "'");
        rs.close();
        stmt.close();
        con.close();

    }

    private static void updateStarForSpillingOverStory() throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append("  select storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where iterationChanged=1) ");
        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), -20, "For story spill over:" + rs.getString(1), "Spillover Champ");
        }
        buffer = new StringBuilder();
        buffer.append("  select storyNumber,email from user cross join storyHistory where iterationChanged=1 and leadandabove=1 ");
        rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), -5, "For story spill over:" + rs.getString(1), "Spillover Champ");
        }

        stmt.execute("update storyHistory set iterationChanged=0,spillover=1 where iterationChanged=1");
        rs.close();
        stmt.close();
        con.close();
    }

    private static void updateStarForNotUpdatingStoryPlannedEstimates(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append(" select storyNumber,storyOwner from storyHistory where planEstimate=0 and iteration='" + iteration + "' ");
        buffer.append("  union all ");
        buffer.append("  select storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where planEstimate=0 and iteration='" + iteration + "') ");
        buffer.append("  union all ");
        buffer.append("  select storyNumber,email from user cross join storyHistory where planEstimate=0 and iteration='" + iteration + "' and leadandabove=1 ");

        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), -2, "Not having planned estimates on story:" + rs.getString(1), "Process Violator");
        }
        rs.close();
        stmt.close();
        con.close();
    }

    private static void updateStarForUpdatingStoryPlannedEstimates(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append(" select storyNumber,storyOwner from storyHistory where planEstimateChanged=1 and iteration='" + iteration + "' ");
        buffer.append("  union all ");
        buffer.append("  select storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where planEstimateChanged=1 and iteration='" + iteration + "') ");

        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), 1, "For having planned estimates on story:" + rs.getString(1), "Process Champ");
        }
        stmt.execute("update storyHistory set planEstimateChanged=0 where planEstimateChanged=1 and iteration='" + iteration + "'");
        rs.close();
        stmt.close();
        con.close();
    }

    private static void updateStarForGettingStoryAcceptedInIteration(String iteration, String date) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder buffer = new StringBuilder();
        buffer.append(" select distinct storyNumber,storyOwner,stateChanged,spillover from storyHistory where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "' ");
        buffer.append("  union distinct ");
        buffer.append("  select distinct storyNumber,storyTaskOwner,stateChanged,spillover from storyUsers inner join (	select storyNumber,stateChanged,spillover from storyHistory where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "') as a using (storyNumber) ");


        ResultSet rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            int stars = getStarsWhenStoryCompletedAndAcceptedInTheSameDay(rs);
            stars = getBonusStarsForAcceptingTheStory(stars, date, rs.getInt(4));

            post(rs.getString(2), stars, "For getting story Accepted:" + rs.getString(1), "Delivery Champ");
        }
        buffer = new StringBuilder();
        buffer.append("  select storyNumber,email from user cross join (select storyNumber from storyHistory where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "' ) as a where leadandabove=1");
        rs = stmt.executeQuery(buffer.toString());
        while (rs.next()) {
            post(rs.getString(2), 1, "For team getting story Accepted:" + rs.getString(1), "Delivery Champ");
        }

        stmt.execute("update storyHistory set stateChanged=0 where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "'");
        rs.close();
        stmt.close();
        con.close();
    }

    protected static int getBonusStarsForAcceptingTheStory(int stars, String date, int spillover) {
        if (spillover == 1)
            return stars;
        int noOfDaysPastInIteration = workingDaysSinceStartOfIteration.indexOf(date);
        TreeMap<Integer, Integer> dayWiseScore = new TreeMap<Integer, Integer>() {{
            put(2, 25);
            put(4, 20);
            put(6, 15);
            put(Integer.MAX_VALUE, 10);
        }};
        Map.Entry<Integer, Integer> closestMatch = dayWiseScore.ceilingEntry(noOfDaysPastInIteration);
        return closestMatch.getValue() + stars;
    }

    private static int getStarsWhenStoryCompletedAndAcceptedInTheSameDay(
            ResultSet rs) throws Exception {
        return (rs.getInt(3) - 1) * 10;
    }

    private static int post(String urlParameters, String url) {
        int responseCode;
        try {
            System.out.println("\nSending 'POST' request to URL : " + RallyConfiguration.postUrl + "/trophy/save");
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Content-Length", "" + urlParameters.length());

            // Send post request
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();
            responseCode = con.getResponseCode();
            System.out.println("Response Code:" + responseCode);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (Exception e) {
            System.out.println("Could not post the score:" + e.getMessage());
            e.printStackTrace();
            return 500;
        }
        return responseCode;
    }

    private static boolean post(String emailId, int score, String reason, String badge) {

        String urlParameters = "{ \"fromUserEmailID\":\"" + RallyConfiguration.postEmail + "\", \"toUserEmailID\":\"" +
                emailId + "\"" +
                ",\"trophies\":" +
                score +
                ",\"badgeName\":" +
                "\"" + badge + "\"" +
                ",\"reason\":\"" + reason + "\"}";

        System.out.println("Post parameters : " + urlParameters);
        int responseCode = post(urlParameters, RallyConfiguration.postUrl);

        if (responseCode == 500) {
            System.out.println("Attempting alternate URL.");
            responseCode = post(urlParameters, RallyConfiguration.alternatePostUrl);
        }

        return responseCode == 200;
    }

    private static void updateStarForNotUpdatingRally(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" select taskOwner,group_concat(taskNumber) from taskHistory where ");
        stringBuilder.append(" taskOwner in ");
        stringBuilder.append(" ( ");
        stringBuilder.append(" select distinct taskOwner from taskHistory where taskOwner not in ( ");
        stringBuilder.append(" 	select distinct taskOwner from taskHistory where taskChanged=1 and iteration='" + iteration + "' ");
        stringBuilder.append(" ) and iteration = '" + iteration + "' and state<>'Completed' ");
        stringBuilder.append(" ) ");
        stringBuilder.append(" and iteration = '" + iteration + "' ");
        stringBuilder.append(" and state<>'Completed' ");
        stringBuilder.append(" group by 1 ");

        ResultSet rs = stmt.executeQuery(stringBuilder.toString());
        while (rs.next()) {
            post(rs.getString(1), -5, "For not updating either of tasks " + rs.getString(2), "Process Violator");
        }
        rs.close();
        stmt.close();
        con.close();
    }

    private static void updateStarForUpdatingRally(String iteration) throws Exception {
        Connection con = RallyConfiguration.getConnection();
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("select taskOwner,group_concat(taskNumber) from taskHistory where taskChanged=1 and iteration='" + iteration + "' group by 1");
        while (rs.next()) {
            post(rs.getString(1), 1, "For updating tasks " + rs.getString(2), "Process Champ");
        }
        stmt.execute("update taskHistory set taskChanged=0 where iteration='" + iteration + "'");
        rs.close();
        stmt.close();
        con.close();
    }
}