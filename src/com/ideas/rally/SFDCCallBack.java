package com.ideas.rally;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * Created by idnasr on 1/5/2015.
 */
public interface SFDCCallBack {
        public boolean procesResult(JsonElement jsonElement, List<String> input, List<String> output) throws Exception;

}

