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

import java.sql.ResultSet;
import java.util.*;

import static com.ideas.rally.SQLExecutor.executeQuery;

public class Iteration {
    private final List<String> workingDaysSinceStartOfIteration = new ArrayList<String>();
    private final String currentDate;
    private final List<String> holidays;

    public Iteration(String currentDate, List<String> holidays) {
        this.currentDate = currentDate;
        this.holidays = holidays;
    }

    private String getIteration(String date) throws Exception {
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

    private String subtractOneDay(String date) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(RallyRockStarIntegration.sdf.parse(date));
        calendar.add(Calendar.DATE, -1);
        return RallyRockStarIntegration.sdf.format(calendar.getTime());
    }

    protected void populateWorkingDaysSinceStartOfIteration(String startDate, String endDate) throws Exception {
        Calendar startCalDate = Calendar.getInstance();
        startCalDate.setTime(RallyRockStarIntegration.sdf.parse(startDate));
        Calendar endCalDate = Calendar.getInstance();
        endCalDate.setTime(RallyRockStarIntegration.sdf.parse(endDate));
        while (!startCalDate.after(endCalDate)) {
            if (startCalDate.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && startCalDate.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY && !holidays.contains(RallyRockStarIntegration.sdf.format(startCalDate.getTime()))) {
                workingDaysSinceStartOfIteration.add(RallyRockStarIntegration.sdf.format(startCalDate.getTime()));
            }
            startCalDate.add(Calendar.DATE, 1);
        }
    }

    private String getPreviousIteration(String date) throws Exception {
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
                    Date dStartDate = RallyRockStarIntegration.sdf.parse(startDate);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(dStartDate);
                    cal.add(Calendar.DATE, -1);
                    return getIteration(RallyRockStarIntegration.sdf.format(cal.getTime()));
                }
            }

        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);

        return null;

    }

    private void getTasks(String iteration) throws Exception {
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

    private float getFloatValue(JsonObject json, String value) {
        if (json == null || json.get(value) instanceof JsonNull || json.get(value) == null) {
            return 0.0f;
        }
        return json.get(value).getAsFloat();
    }

    private void getStories(String iteration, String storyDefect) throws Exception {
        getStoryDefect(storyDefect, "Iteration.Name", iteration, iteration);
        updateStoryIteration(iteration);
    }

    private void getStoryDefect(String storyDefect, String filter, String filterValue, String iteration)
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

    private void getStoryIteration(String storyDefect, String storyNumber, String expectedIteration) throws Exception {
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

    private void updateStoryIterationNumber(String storyNumber, String iterationName) throws Exception {
        executeQuery("insert into storyhistory(storyNumber,iteration) values ('" + storyNumber + "','" + iterationName + "') on duplicate key update iterationChanged=if(VALUES(iteration) = 'null',0,if(VALUES(iteration) <> iteration,1,0)), iteration=VALUES(iteration)");
    }

    private void updateStoryIteration(final String iteration) throws Exception {
        new SQLExecutor("select storyNumber from storyhistory where iteration='" + iteration + "'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                String storyNumber = rs.getString(1);
                String storyType = storyNumber.startsWith("US") ? "HierarchicalRequirement" : "Defect";
                getStoryIteration(storyType, storyNumber, iteration);
            }
        }.go();
    }

    private void deleteStoryTaskOwners(String storyNumber) throws Exception {
        executeQuery("delete from storyUsers where storyNumber='" + storyNumber + "'");
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
                String email = getUserEmailAddress(childObject.getAsJsonObject("Owner").get("_refObjectName").getAsString());
                if (email != null) {
                    insertIntoStoryUsers(storyNumber, email);
                }
            }
        }
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
        executeQuery(buffer.toString());
    }

    private void insertIntoStoryUsers(String storyNumber, String emailAddress) throws Exception {
        executeQuery("insert into storyUsers(storyNumber,storyTaskOwner) " + " values ('" + storyNumber + "','" + emailAddress + "') " + " on duplicate key update " + " storyTaskOwner=VALUES(storyTaskOwner) ");
    }

    private String getUserEmailAddress(String owner) throws Exception {
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

    private void updateEmail(String owner, String email) throws Exception {
        executeQuery("insert ignore into user(userName,email) values('" + owner + "','" + email + "')");
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

    private String getReferenceName(JsonObject json, String name) {
        if (json.get(name) instanceof JsonNull)
            return null;
        JsonObject owner = json.get(name).getAsJsonObject();
        return owner.get("_refObjectName").getAsString();
    }

    private void insertIntoTaskHistory(String iteration, String taskNumber, String taskOwner, float actual, float todo, String state) throws Exception {
        StringBuilder buffer = new StringBuilder()
                .append(" insert into taskHistory(iteration,taskNumber,taskOwner,actuals,toDo,state,taskChanged) ")
                .append(" values ('" + iteration + "','" + taskNumber + "','" + taskOwner + "'," + actual + "," + todo + ",'" + state + "',1) ")
                .append(" on duplicate key update ")
                .append(" taskChanged=if(actuals<>VALUES(actuals),1,if(toDo<>VALUES(toDo),1,if(state<>VALUES(state),1,0))), ")
                .append(" actuals=VALUES(actuals), ")
                .append(" toDo=VALUES(toDo), ")
                .append(" state=VALUES(state) ");
        executeQuery(buffer.toString());
    }

    public void execute() throws Exception {
        if (!RallyConfiguration.post(RallyConfiguration.postEmail, 0, "Test Connection", "Default"))
            throw new RuntimeException("Cannot connect to the Rally");

        String iteration = getIteration(currentDate);
        String previousIteration = getPreviousIteration(currentDate);
        System.out.println("Got iteration as:" + iteration + " for yesterday:" + currentDate);

        if (iteration == null || iteration.equals("null")) return;

        getTasks(iteration);
        cleanUpDeletedTasks(iteration);

        getStories(iteration, "HierarchicalRequirement");
        getStories(iteration, "Defect");
        cleanUpDeletedStoriesDefects(iteration);
        updateStatusOfUnacceptedStoriesInPreviousIteration(previousIteration);

        RallyConfiguration.optimizeTables();

        updateStarForNotUpdatingStoryPlannedEstimates(iteration);
        updateStarForUpdatingStoryPlannedEstimates(iteration);
        updateStarForSpillingOverStory();

        updateStarForGettingStoryAcceptedInIteration(iteration, currentDate);
        updateStarForCompletingStoryInIteration(iteration);
        updateStarForNotTaskingStory(iteration);

        updateStarForLeavingStoryInPriorIterationsWhichIsNotAccepted(previousIteration);
        updateStarForNotUpdatingRally(iteration);
        updateStarForUpdatingRally(iteration);
    }

    private void updateStatusOfUnacceptedStoriesInPreviousIteration(final String previousIteration) throws Exception {
        new SQLExecutor("select storyNumber, storyHistory.iteration, state from storyHistory where iteration='" + previousIteration + "' and state<>'Accepted'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                String storyNumber = rs.getString(1);
                String storyType = storyNumber.startsWith("US") ? "HierarchicalRequirement" : "Defect";
                getStoryDefect(storyType, "FormattedID", storyNumber, previousIteration);
            }
        }.go();
    }

    private void cleanUpDeletedStoriesDefects(String iteration) throws Exception {
        System.out.println("iteration:" + iteration);
        new SQLExecutor("select storyNumber from storyhistory where iteration='" + iteration + "'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                cleanUpStoriesIfRecycled(rs.getString(1));
            }
        }.go();
    }

    private void cleanUpStoriesIfRecycled(String storyDefectNumber) throws Exception {
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

    private void cleanUpDeletedTasks(String iteration) throws Exception {
        System.out.println("iteration:" + iteration);
        new SQLExecutor("select taskNumber from taskHistory where iteration='" + iteration + "'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                cleanUpTaskIfRecycled(rs.getString(1));
            }
        }.go();
    }

    private void cleanUpTaskIfRecycled(String taskNumber)
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

    private void deleteTaskNumber(String taskNumber) throws Exception {
        executeQuery("delete from taskHistory where taskNumber='" + taskNumber + "'");
    }

    private void deleteStoryNumber(String storyNumber) throws Exception {
        executeQuery(
                "delete from storyhistory where storyNumber='" + storyNumber + "'",
                "delete from storyusers where storyNumber='" + storyNumber + "'"
        );
    }

    private void updateStarForLeavingStoryInPriorIterationsWhichIsNotAccepted(String iteration) throws Exception {
        new SQLExecutor("select storyNumber,storyTaskOwner,storyHistory.iteration,state from storyHistory left join storyUsers using (storyNumber) where iteration='" + iteration + "'  and state<>'Accepted'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), -5, "For leaving the story/defect :" + rs.getString(1) + " in iteration :" + rs.getString(3) + " in state " + rs.getString(4), "Process Violator");
            }
        }.go();
    }

    private void updateStarForNotTaskingStory(String iteration) throws Exception {
        new SQLExecutor("select storyNumber,storyOwner,storyTaskOwner from storyHistory left join storyUsers using (storyNumber) where iteration='" + iteration + "' group by 1  having storyTaskOwner is null ") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), -5, "For not tasking story:" + rs.getString(1), "Process Violator");
            }
        }.go();


        new SQLExecutor("select storyNumber,email from user cross join (select storyNumber from storyHistory left join storyUsers using (storyNumber) where iteration='" + iteration + "' and storyTaskOwner is null group by 1) as a where leadandabove=1") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), -2, "For not tasking story:" + rs.getString(1), "Process Violator");
            }
        }.go();
    }

    private void updateStarForCompletingStoryInIteration(String iteration) throws Exception {
        new SQLExecutor("select distinct storyNumber,storyOwner from storyHistory where stateChanged=1 and state='Completed' and iteration='" + iteration + "' " + "  union DISTINCT select distinct storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where stateChanged=1 and state='Completed' and iteration='" + iteration + "') ") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), 10, "For completing the story:" + rs.getString(1), "Delivery Champ");
            }
        }.go();

        executeQuery("update storyHistory set stateChanged=0 where stateChanged=1 and state='Completed' and iteration='" + iteration + "'");
    }

    private void updateStarForSpillingOverStory() throws Exception {
        new SQLExecutor("select storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where iterationChanged=1)") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), -20, "For story spill over:" + rs.getString(1), "Spillover Champ");
            }
        }.go();

        new SQLExecutor("select storyNumber,email from user cross join storyHistory where iterationChanged=1 and leadandabove=1") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), -5, "For story spill over:" + rs.getString(1), "Spillover Champ");
            }
        }.go();
    }

    private void updateStarForNotUpdatingStoryPlannedEstimates(String iteration) throws Exception {
        new SQLExecutor("select storyNumber,storyOwner from storyHistory where planEstimate=0 and iteration='" + iteration + "' " + "  union all " + "  select storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where planEstimate=0 and iteration='" + iteration + "') " + "  union all " + "  select storyNumber,email from user cross join storyHistory where planEstimate=0 and iteration='" + iteration + "' and leadandabove=1") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), -2, "Not having planned estimates on story:" + rs.getString(1), "Process Violator");
            }
        }.go();
    }

    private void updateStarForUpdatingStoryPlannedEstimates(String iteration) throws Exception {
        new SQLExecutor("select storyNumber,storyOwner from storyHistory where planEstimateChanged=1 and iteration='" + iteration + "' " + "  union all " + "  select storyNumber,storyTaskOwner from storyUsers where storynumber in (select storyNumber from storyHistory where planEstimateChanged=1 and iteration='" + iteration + "') ") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), 1, "For having planned estimates on story:" + rs.getString(1), "Process Champ");
            }
        }.go();

        executeQuery("update storyHistory set planEstimateChanged=0 where planEstimateChanged=1 and iteration='" + iteration + "'");
    }

    private void updateStarForGettingStoryAcceptedInIteration(String iteration, final String date) throws Exception {
        new SQLExecutor("select distinct storyNumber,storyOwner,stateChanged,spillover from storyHistory where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "' " + "  union distinct " + "  select distinct storyNumber,storyTaskOwner,stateChanged,spillover from storyUsers inner join (	select storyNumber,stateChanged,spillover from storyHistory where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "') as a using (storyNumber) ") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                int stars = getStarsWhenStoryCompletedAndAcceptedInTheSameDay(rs);
                stars = getBonusStarsForAcceptingTheStory(stars, date, rs.getInt(4));
                RallyConfiguration.post(rs.getString(2), stars, "For getting story Accepted:" + rs.getString(1), "Delivery Champ");
            }
        }.go();

        new SQLExecutor("select storyNumber,email from user cross join (select storyNumber from storyHistory where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "' ) as a where leadandabove=1") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(2), 1, "For team getting story Accepted:" + rs.getString(1), "Delivery Champ");
            }
        }.go();

        executeQuery("update storyHistory set stateChanged=0 where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "'");
    }

    protected int getBonusStarsForAcceptingTheStory(int stars, String date, int spillover) {
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

    private int getStarsWhenStoryCompletedAndAcceptedInTheSameDay(
            ResultSet rs) throws Exception {
        return (rs.getInt(3) - 1) * 10;
    }

    private void updateStarForNotUpdatingRally(String iteration) throws Exception {
        new SQLExecutor("select taskOwner,group_concat(taskNumber) from taskHistory where " + " taskOwner in " + " ( " + " select distinct taskOwner from taskHistory where taskOwner not in ( " + " 	select distinct taskOwner from taskHistory where taskChanged=1 and iteration='" + iteration + "' " + " ) and iteration = '" + iteration + "' and state<>'Completed' " + " ) " + " and iteration = '" + iteration + "' " + " and state<>'Completed' " + " group by 1 ") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(1), -5, "For not updating either of tasks " + rs.getString(2), "Process Violator");
            }
        }.go();
    }

    private void updateStarForUpdatingRally(String iteration) throws Exception {
        new SQLExecutor("select taskOwner,group_concat(taskNumber) from taskHistory where taskChanged=1 and iteration='" + iteration + "' group by 1") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                RallyConfiguration.post(rs.getString(1), 1, "For updating tasks " + rs.getString(2), "Process Champ");
            }
        }.go();
        executeQuery("update taskHistory set taskChanged=0 where iteration='" + iteration + "'");
    }
}