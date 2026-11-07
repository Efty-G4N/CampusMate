package com.eftyg4n.campusmate.utils;

import java.util.UUID;

public class IdUtils {

    private IdUtils() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}