package com.eftyg4n.campusmate.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MarksUtilsTest {

    @Test
    public void percentOfZeroTotalIsZero() {
        assertEquals(0.0, MarksUtils.percent(5, 0), 0.0001);
    }

    @Test
    public void percentIsObtainedOverTotal() {
        assertEquals(75.0, MarksUtils.percent(15, 20), 0.0001);
    }

    @Test
    public void bestAverageTakesTheTopScores() {
        // best 3 of {50, 90, 70, 80} = (90 + 80 + 70) / 3
        assertEquals(80.0, MarksUtils.bestAveragePercent(new double[]{50, 90, 70, 80}, 3), 0.0001);
    }

    @Test
    public void bestAverageWithFewerScoresUsesAllOfThem() {
        assertEquals(60.0, MarksUtils.bestAveragePercent(new double[]{50, 70}, 3), 0.0001);
    }

    @Test
    public void bestAverageOfNothingIsZero() {
        assertEquals(0.0, MarksUtils.bestAveragePercent(new double[0], 3), 0.0001);
    }

    @Test
    public void parseNumberAcceptsCommaAndRejectsText() {
        assertEquals(7.5, MarksUtils.parseNumber("7,5"), 0.0001);
        assertEquals(-1.0, MarksUtils.parseNumber("abc"), 0.0001);
        assertEquals(-1.0, MarksUtils.parseNumber(""), 0.0001);
    }

    @Test
    public void plainDropsTrailingZero() {
        assertEquals("8", MarksUtils.plain(8.0));
        assertEquals("7.50", MarksUtils.plain(7.5));
    }
}
