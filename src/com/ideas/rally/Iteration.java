package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.sql.ResultSet;
import java.util.*;

import static com.ideas.rally.SQLExecutor.executeUpdate;

public class Iteration {
    private final List<String> workingDaysSinceStartOfIteration = new ArrayList<String>();
    private final String currentDate;
    private final List<String> holidays;

    public Iteration(String currentDate, List<String> holidays) {
        this.currentDate = currentDate;
        this.holidays = holidays;
    }

    public void execute() throws Exception {
        if (!RallyConfiguration.post(RallyConfiguration.postEmail, 0, "Test Connection", "Default"))
            throw new RuntimeException("Cannot connect to the Rock Star App");

        String iteration = getIterationName(currentDate, new CurrentIteration());
        String previousIteration = getIterationName(currentDate, new LastIteration());
        System.out.println("Got iteration as:" + iteration + " for yesterday:" + currentDate);

        if (iteration == null) return;

        updateTaskDetails(iteration);
        cleanUpDeletedTasks(iteration);

        updateStoryDetails(iteration, "HierarchicalRequirement");
        updateStoryDetails(iteration, "Defect");
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

    private String getIterationName(String date, IterationType iterationType) throws Exception {
        Fetch fetchList = new Fetch("Name","StartDate", "EndDate" );
        QueryFilter queryFilter = new QueryFilter("Project", "=", RallyConfiguration.RALLY_PROJECT);
        CallBack callBack = new IterationCallBack(iterationType);
        Executor executor = new Executor("Iteration",fetchList,queryFilter, callBack, date);
        List<String> output = executor.execute();
        if(output.size() > 0) {
            if(iterationType.populateWorkingDays())
                populateWorkingDaysSinceStartOfIteration(output.get(1), date);
            return output.get(0);
        }
        return null;
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

    private void updateTaskDetails(String iteration) throws Exception {
        System.out.println("iteration:" + iteration);

        Fetch fetch = new Fetch("FormattedID", "Actuals", "Blocked", "State", "ToDo", "Owner" );
        QueryFilter queryFilter = new QueryFilter("Iteration.Name", "=", iteration);
        Executor executor = new Executor("Task",fetch,queryFilter,new TaskCallBack(),iteration);
        executor.execute();
    }

    private void updateStoryDetails(String iteration, String storyType) throws Exception {
        System.out.println("iteration:" + iteration);

        Fetch fetch = new Fetch("FormattedID", "ScheduleState", "PlanEstimate", "Tasks", "Owner" );
        QueryFilter queryFilter = new QueryFilter("Iteration.Name", "=", iteration);
        Executor executor = new Executor(storyType,fetch,queryFilter, new EstimateCallBack(), iteration);
        executor.execute();
        updateStoryIteration(iteration);
    }

    private void updateStoryIteration(final String iteration) throws Exception {
        new SQLExecutor("select storyNumber from storyhistory where iteration='" + iteration + "'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                String storyNumber = rs.getString(1);
                Fetch fetch = new Fetch("FormattedID", "Actuals", "Blocked", "State", "ToDo", "Owner" );
                QueryFilter queryFilter = new QueryFilter("FormattedID", "=", storyNumber);
                Executor executor = new Executor("Iteration",fetch,queryFilter, new StoryIterationCallBack(), storyNumber, iteration);
                executor.execute();
            }

        }.go();
    }

    private void updateStatusOfUnacceptedStoriesInPreviousIteration(final String previousIteration) throws Exception {
        new SQLExecutor("select storyNumber, storyHistory.iteration, state from storyHistory where iteration='" + previousIteration + "' and state<>'Accepted'") {
            @Override
            public void accept(ResultSet rs) throws Exception {
                String storyNumber = rs.getString(1);
                String storyType = storyNumber.startsWith("US") ? "HierarchicalRequirement" : "Defect";
                System.out.println("iteration:" + previousIteration);

                Fetch fetch = new Fetch("FormattedID", "ScheduleState", "PlanEstimate", "Tasks", "Owner" );
                QueryFilter queryFilter = new QueryFilter("FormattedID", "=", storyNumber);
                Executor executor = new Executor(storyType,fetch,queryFilter, new EstimateCallBack(), previousIteration);
                executor.execute();
                updateStoryIteration(previousIteration);
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

    private void cleanUpTaskIfRecycled(String taskNumber) throws Exception {
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
        executeUpdate("delete from taskHistory where taskNumber='" + taskNumber + "'");
    }

    private void deleteStoryNumber(String storyNumber) throws Exception {
        executeUpdate(
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

        executeUpdate("update storyHistory set stateChanged=0 where stateChanged=1 and state='Completed' and iteration='" + iteration + "'");
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

        executeUpdate("update storyHistory set planEstimateChanged=0 where planEstimateChanged=1 and iteration='" + iteration + "'");
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

        executeUpdate("update storyHistory set stateChanged=0 where stateChanged>=1 and state='Accepted' and iteration='" + iteration + "'");
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
        executeUpdate("update taskHistory set taskChanged=0 where iteration='" + iteration + "'");
    }
}