//
// ShellFormatter
//
//
// Created by aleross on 3/1/13
// Copyright (c) 2012 Enplug, Inc. All rights reserved.
//

package com.aleross.shellcommand;

import java.util.HashMap;
import java.util.List;

public abstract class ShellResponseFormatter {

    private static final HashMap<String, String> FORMATS = new HashMap<String, String>(5);

    static {
        FORMATS.put("*", "     "); // 5 spaces for topic headers
        FORMATS.put("**", "          "); // 10 spaces for sub-items
    }

    public static String formatSectionHeader(final String sectionName) {
        return "---- " + sectionName + " ----";
    }

    public static List<String> formatResponse(final List<String> response) {
        for (int i = 0; i < response.size(); i++) {
            String string = response.get(i);
            final String topic = string.substring(0, 1);
            final String item = string.substring(0, 2);
            // Make sure to check for item first
            if (FORMATS.containsKey(item)) {
                string = string.replaceFirst("\\" + item, FORMATS.get(item));
                response.set(i, string);
            } else if (FORMATS.containsKey(topic)) {
                string = string.replaceFirst("\\" + topic, FORMATS.get(topic));
                response.set(i, string);
            }
        }
        return response;
    }
}

