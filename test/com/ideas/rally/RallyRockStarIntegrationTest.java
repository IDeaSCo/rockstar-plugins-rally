package com.ideas.rally;

import com.ideas.rally.RallyConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RallyRockStarIntegrationTest {
    private static int stars = 10;
    private final Iteration iteration = new Iteration("2014-12-22", Collections.<String>emptyList());
    @BeforeClass
    public static void beforeClass() throws Exception {
        RallyConfiguration.testRun=true;
        RallyConfiguration.createSchema();
    }
    @Test
    public void noPointsWhenStorySpillsOver() throws Exception {
        int bonus = iteration.getBonusStarsForAcceptingTheStory(stars, "2014-12-22", 1);
        assertEquals(stars, bonus);
    }

    @Test
    public void giveBonusPointsForAcceptingStoryEarly() throws Exception {
        int startDate = 15;
        iteration.populateWorkingDaysSinceStartOfIteration("2014-12-" + startDate, "2014-12-31");
        Map<Integer, Integer> dayWiseScore = new HashMap<Integer, Integer>() {{
            put(2, 25);
            put(4, 20);
            put(8, 15);
            put(10, 10);
        }};
        for (Map.Entry<Integer, Integer> entry : dayWiseScore.entrySet()) {
            int currentDate = startDate + entry.getKey() - 1;
            int bonus = iteration.getBonusStarsForAcceptingTheStory(stars, "2014-12-" + currentDate, 0);
            assertEquals(stars + entry.getValue(), bonus);
        }
    }


}
