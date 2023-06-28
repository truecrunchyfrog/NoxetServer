package org.noxet.noxetserver.util;

public class FancyTimeConverter {
    public static String deltaSecondsToFancyTime(int seconds) {
        return deltaSecondsToFancyTime(seconds, false);
    }

    public static String deltaSecondsToFancyTime(int seconds, boolean secondSpecific) {
        int days = seconds / 86400,
            hours = seconds / 3600 % 24,
            minutes = seconds / 60 % 60;
        seconds %= 60;

        StringBuilder stringBuilder = new StringBuilder();

        if(days > 0)
            stringBuilder.append(days).append("d ");

        if(hours > 0)
            stringBuilder.append(hours).append("h ");

        if(days == 0 && minutes > 0)
            stringBuilder.append(minutes).append("m ");

        if(days == 0 && hours == 0 && (secondSpecific || minutes == 0) && seconds > 0)
            stringBuilder.append(seconds).append("s ");

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);

        return stringBuilder.toString();
    }
}
