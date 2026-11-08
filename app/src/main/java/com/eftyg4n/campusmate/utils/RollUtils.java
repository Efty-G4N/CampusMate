package com.eftyg4n.campusmate.utils;

public class RollUtils {

    private RollUtils() {
    }

    // Serial number = last "digits" digits of the roll number.
    // Example: roll 2307100 with digits 3 -> 100. Returns -1 if it is not valid.
    public static int serialOf(String rollNo, int digits) {
        if (rollNo == null || digits <= 0) {
            return -1;
        }
        String roll = rollNo.trim();
        if (roll.length() < digits) {
            return -1;
        }
        String tail = roll.substring(roll.length() - digits);
        try {
            return Integer.parseInt(tail);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}