package me.sedattr.deluxeauctions.others;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import org.bukkit.configuration.ConfigurationSection;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class NumberFormat {
    private DecimalFormat format = new DecimalFormat();
    Map<String, Double> suffixes = new HashMap<>();
    String type = "";
    List<Map.Entry<String, Double>> sorted = new ArrayList<>();

    public NumberFormat() {
        ConfigurationSection section = DeluxeAuctions.getInstance().configFile.getConfigurationSection("number_format");
        if (section != null) {
            this.type = section.getString("type", "").toLowerCase(Locale.ENGLISH);

            this.format = new DecimalFormat();
            this.format.setRoundingMode(RoundingMode.HALF_UP);
            this.format.setMinimumFractionDigits(section.getInt("decimal_settings.minimum_fraction"));
            this.format.setMaximumFractionDigits(section.getInt("decimal_settings.maximum_fraction"));

            this.suffixes.put("", 1.0);
            this.suffixes.put(section.getString("short_settings.thousand", "k"), 1_000.0);
            this.suffixes.put(section.getString("short_settings.million", "M"), 1_000_000.0);
            this.suffixes.put(section.getString("short_settings.billion", "B"), 1_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.trillion", "T"), 1_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.quadrillion", "P"), 1_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.quintillion", "E"), 1_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.sextillion", "Z"), 1_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.septillion", "Y"), 1_000_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.octillion", "R"), 1_000_000_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.nonillion", "Q"), 1_000_000_000_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.decillion", "Dc"), 1_000_000_000_000_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.undecillion", "Ud"), 1_000_000_000_000_000_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.duodecillion", "Dd"), 1_000_000_000_000_000_000_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.tredecillion", "Td"), 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000.0);
            this.suffixes.put(section.getString("short_settings.quattuordecillion", "Qd"), 1_000_000_000_000_000_000_000_000_000_000_000_000_000_000_000.0);

            sort();
        }
    }

    private void sort() {
        this.sorted = new ArrayList<>(this.suffixes.entrySet());
        this.sorted.sort(Map.Entry.comparingByValue());
    }

    public Double reverseFormat(String text) {
        if (text == null || text.isEmpty())
            return 0.0;

        text = text.toLowerCase(Locale.ENGLISH);

        double highest = 0;
        for (Map.Entry<String, Double> entry : this.sorted) {
            String suffix = entry.getKey().toLowerCase(Locale.ENGLISH);
            double multi = entry.getValue();

            if (text.contains(suffix))
                try {
                    double number = Double.parseDouble(text.replace(suffix, "")) * multi;
                    if (number > highest)
                        highest = number;
                } catch (NumberFormatException ignored) {
                }
        }

        if (highest > 0)
            return highest;

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public String format(Double number) {
        if (number == null)
            return "";

        if (number < 0)
            return "-" + format(-number);

        switch (this.type) {
            case "short":
                if (number < 1_000)
                    return String.valueOf(number);

                double highestDivisor = 0;
                String suffix = "";

                for (Map.Entry<String, Double> entry : this.sorted) {
                    double divisor = entry.getValue();
                    if (number >= divisor && divisor >= highestDivisor) {
                        highestDivisor = divisor;
                        suffix = entry.getKey();
                    }
                }

                if (suffix.isEmpty() || highestDivisor <= 0.0)
                    return String.valueOf(number);

                double shortNumber = number / highestDivisor;

                DecimalFormat shortFormat = new DecimalFormat();
                shortFormat.setRoundingMode(RoundingMode.HALF_UP);
                shortFormat.setMinimumFractionDigits(0);
                shortFormat.setMaximumFractionDigits(2);
                shortFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));

                return shortFormat.format(shortNumber) + suffix;
            case "decimal":
                if (this.format == null)
                    return number.toString();

                return this.format.format(number);
            default:
                return number.toString();
        }
    }
}