package com.eftyg4n.campusmate.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AttendanceUtilsTest {

    @Test
    public void percentOfNothingIsZero() {
        assertEquals(0.0, AttendanceUtils.percent(0, 0), 0.0001);
    }

    @Test
    public void percentIsAttendedOverTotal() {
        assertEquals(80.0, AttendanceUtils.percent(8, 10), 0.0001);
    }

    @Test
    public void canMissCountsFutureAbsences() {
        // 14 of 17 now. Missing one more gives 14/18 = 77.7% (fine), two gives 14/19 = 73.7%.
        assertEquals(1, AttendanceUtils.canMiss(14, 17, 75));
    }

    @Test
    public void canMissIsZeroWhenAlreadyBelow() {
        assertEquals(0, AttendanceUtils.canMiss(10, 15, 75));
    }

    @Test
    public void needToAttendGetsBackToTheLine() {
        // 10 of 15 (66.7%). Five more in a row: 15/20 = 75%.
        assertEquals(5, AttendanceUtils.needToAttend(10, 15, 75));
        assertFalse(AttendanceUtils.isOnTrack(10, 15, 75));
        assertTrue(AttendanceUtils.isOnTrack(15, 20, 75));
    }

    @Test
    public void needToAttendIsZeroWhenOnTrack() {
        assertEquals(0, AttendanceUtils.needToAttend(9, 10, 75));
    }

    @Test
    public void hundredPercentCannotBeRecovered() {
        assertEquals(-1, AttendanceUtils.needToAttend(9, 10, 100));
        assertEquals(0, AttendanceUtils.needToAttend(10, 10, 100));
    }

    @Test
    public void emptyHistoryIsOnTrack() {
        assertTrue(AttendanceUtils.isOnTrack(0, 0, 75));
    }
}
