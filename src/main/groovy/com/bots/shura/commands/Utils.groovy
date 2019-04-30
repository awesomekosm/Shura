package com.bots.shura.commands

import org.apache.commons.lang3.StringUtils

class Utils {
    static List<String> parseCommands(String input, int max) {
        input = StringUtils.trim(input)
        return Arrays.asList(StringUtils.split(input, " ", max))
    }
}
