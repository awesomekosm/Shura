package com.bots.shura.commands;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class Utils {
    public static List<String> parseCommands(String input, int max) {
        input = StringUtils.trim(input);
        return Arrays.asList(StringUtils.split(input, " ", max));
    }
}
