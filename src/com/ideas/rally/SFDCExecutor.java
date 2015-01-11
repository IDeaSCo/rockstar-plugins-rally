package com.ideas.rally;

import com.google.gson.JsonArray;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.util.ArrayList;
import java.util.List;

public class SFDCExecutor {
    private final String queryRequest;
    private final Fetch fetch;
    private final QueryFilter queryFilter;
    private final SFDCCallBack callBack;
    private final List<String> input;

    public SFDCExecutor(String queryRequest, Fetch fetch, QueryFilter queryFilter, SFDCCallBack callBack, List<String> input){
        this.queryRequest=queryRequest;
        this.fetch=fetch;
        this.queryFilter= queryFilter;
        this.callBack=callBack;
        this.input=input;
    }

    public List<String> execute() throws Exception {
        List<String> output = new ArrayList<String>();
        RallyRestApi restApi = RallyConfiguration.getRallyRestApi();
        QueryRequest workSpaceRequest = new QueryRequest(queryRequest);
        workSpaceRequest.setFetch(fetch);
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
            output.addAll(callBack.processResult(array, input));

        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);
        return output;
    }
}
