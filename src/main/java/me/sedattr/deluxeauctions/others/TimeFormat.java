package me.sedattr.deluxeauctions.others;

import com.google.common.collect.Maps;
import me.sedattr.deluxeauctions.DeluxeAuctions;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeFormat {
    private final ConfigurationSection section;
    private final Pattern pattern = Pattern.compile("(\\d+)\\s*(\\w+)");
    private HashMap<String, Integer> units = Maps.newHashMap();

    public TimeFormat() {
        this.section = DeluxeAuctions.getInstance().configFile.getConfigurationSection("time_format");

        loadUnits();
    }

    public void loadUnits() {
        ConfigurationSection convertSection = this.section.getConfigurationSection("convert_times");
        if (convertSection == null)
            return;

        HashMap<String, Integer> newUnits = Maps.newHashMap();
        for (String key : convertSection.getKeys(false)) {
            List<String> units = convertSection.getStringList(key);
            if (units.isEmpty())
                continue;

            for (String unit : units) {
                if (key.equalsIgnoreCase("weeks"))
                    newUnits.put(unit, 604800);
                else if (key.equalsIgnoreCase("days"))
                    newUnits.put(unit, 86400);
                else if (key.equalsIgnoreCase("hours"))
                    newUnits.put(unit, 3600);
                else if (key.equalsIgnoreCase("minutes"))
                    newUnits.put(unit, 60);
                else
                    newUnits.put(unit, 1);
            }
        }

        this.units = newUnits;
    }

    public int convertTime(String input) {
        int value = 0;
        final Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            final int modifier = Integer.parseInt(matcher.group(1));
            final String unit = matcher.group(2);
            if (!this.units.containsKey(unit))
                continue;

            value += this.units.get(unit) * modifier;
        }
        return value;
    }

    public String formatTime(Long time, String type) {
        long weeks = time / 604800;
        time %= 604800;
        long days = time / 86400;
        time %= 86400;
        long hours = time / 3600;
        time %= 3600;
        long minutes = time / 60;
        time %= 60;
        long seconds = time;

        if (type == null || type.isEmpty())
            type = "other_times";

        ConfigurationSection timeSection = this.section.getConfigurationSection(type);
        if (timeSection == null)
            return "";

        String text = "";
        if (weeks > 0)
            text = text.concat(weeks + timeSection.getString("weeks", "w") + " ");
        if (days > 0)
            text = text.concat(days + timeSection.getString("days", "d") + " ");
        if (hours > 0)
            text = text.concat(hours + timeSection.getString("hours", "h") + " ");
        if(minutes > 0)
            text = text .concat(minutes + timeSection.getString("minutes", "m") + " ");

        if (seconds > 0) {
            text = text.concat(seconds + timeSection.getString("seconds"));
            return Utils.colorize(text);
        } else if (text.isEmpty())
            return timeSection.getString("ending", "Ending");
        else
            return Utils.colorize(text);
    }
}
