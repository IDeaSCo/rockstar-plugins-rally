package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;

import java.util.List;

/**
 * Created by idnasr on 1/5/2015.
 */
public class SFDCExecutor {
    String queryRequest;
    Fetch fetch;
    QueryFilter queryFilter;
    SFDCCallBack callBack;
    List<String> input;
    List<String> output;

    public SFDCExecutor(String queryRequest,Fetch fetch,QueryFilter queryFilter, SFDCCallBack callBack, List<String> input, List<String> output){
        this.queryRequest=queryRequest;
        this.fetch=fetch;
        this.queryFilter= queryFilter;
        this.callBack=callBack;
        this.input=input;
        this.output=output;
    }

    public void execute() throws Exception {
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
            for (JsonElement jsonElement : array) {
                if(callBack.procesResult(jsonElement,input,output)){

                }else{
                    return;
                }
            }

        } while (workSpaceResponse.getTotalResultCount() > pageCount * 200);
    }
}