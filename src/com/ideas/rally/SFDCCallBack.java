package com.ideas.rally;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.Calendar;
import java.util.List;

/**
 * Created by idnasr on 1/5/2015.
 */
public abstract class SFDCCallBack {
        public abstract void procesResult(JsonArray jsonArray, List<String> input, List<String> output) throws Exception;

        protected String getReferenceName(JsonObject json, String name) {
            if (json.get(name) instanceof JsonNull)
                return null;
            JsonObject owner = json.get(name).getAsJsonObject();
            return owner.get("_refObjectName").getAsString();
        }


        protected String subtractOneDay(String date) throws Exception {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(RallyRockStarIntegration.sdf.parse(date));
            calendar.add(Calendar.DATE, -1);
            return RallyRockStarIntegration.sdf.format(calendar.getTime());
        }

        protected float getFloatValue(JsonObject json, String value) {
            if (json == null || json.get(value) instanceof JsonNull || json.get(value) == null) {
                return 0.0f;
            }
            return json.get(value).getAsFloat();
        }

}

