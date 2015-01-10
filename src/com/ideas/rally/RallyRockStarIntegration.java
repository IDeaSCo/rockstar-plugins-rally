package com.ideas.rally;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RallyRockStarIntegration {
    static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static final List<String> holidays = new ArrayList<String>();

    public static void main(String args[]) throws Exception {
        RallyConfiguration.createSchema();
        loadHolidaysFirstTime();

        Calendar yesterday = yesterday();
        String stringDate = sdf.format(yesterday.getTime());

        if (isWeekEndOrHoliday(yesterday, stringDate)) return;

        new Iteration(stringDate, holidays).execute();
    }

    private static void loadHolidaysFirstTime() {
        if(!holidays.isEmpty()) return;
        try {
            BufferedReader reader = new BufferedReader(new FileReader("holidays.list"));
            String line;
            while ((line = reader.readLine()) != null) {
                holidays.add(line);
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Could not load holidays list.");
            e.printStackTrace();
        }
    }

    private static boolean isWeekEndOrHoliday(Calendar yesterday, String stringDate) {
        return yesterday.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || yesterday.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY || holidays.contains(stringDate);
    }

    private static Calendar yesterday() {
        Calendar date = Calendar.getInstance();
        date.add(Calendar.DATE, -1);
        return date;
    }

}