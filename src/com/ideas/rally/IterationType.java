package com.ideas.rally;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public abstract class IterationType {
    public abstract boolean shouldStop(String iterationName, String givenDate, String startDate, String endDate);
    public abstract List<String> result();
    public abstract boolean populateWorkingDays();

    protected boolean inRange(String givenDate, String startDate, String endDate) {
        return startDate.compareTo(givenDate) <= 0 && endDate.compareTo(givenDate) >= 0;
    }
}

class LastIteration extends IterationType {
    private final List<String> iterations = new ArrayList<String>();

    @Override
    public boolean shouldStop(String iterationName, String givenDate, String startDate, String endDate) {
        if (inRange(givenDate, startDate, endDate)) {
            if(iterations.size() > 0 ) {
                return true;
            }
        }
        iterations.add(iterationName);
        return false;
    }

    @Override
    public List<String> result() {
        return asList(tail(iterations));
    }

    @Override
    public boolean populateWorkingDays() {
        return false;
    }

    private String tail(List<String> list) {
        return list.get(list.size() - 1);
    }
}

class CurrentIteration extends IterationType {
    private String iterationName;
    private String startDate;

    @Override
    public boolean shouldStop(String iterationName, String givenDate, String startDate, String endDate) {
        this.iterationName = iterationName;
        this.startDate = startDate;
        return inRange(givenDate, startDate, endDate);
    }

    @Override
    public List<String> result() {
        return asList(iterationName, startDate);
    }

    @Override
    public boolean populateWorkingDays() {
        return true;
    }
}
