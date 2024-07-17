package me.sedattr.deluxeauctions.others;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import org.bukkit.configuration.ConfigurationSection;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NumberFormat {
    private DecimalFormat format = new DecimalFormat();
    Map<String, Double> suffixes = new HashMap<>();
    String type = "";

    public NumberFormat() {
        ConfigurationSection section = DeluxeAuctions.getInstance().configFile.getConfigurationSection("number_format");
        if (section != null) {
            this.type = section.getString("type", "").toLowerCase();

            this.format = new DecimalFormat();
            this.format.setRoundingMode(RoundingMode.HALF_UP);
            this.format.setMinimumFractionDigits(section.getInt("decimal_settings.minimum_fraction"));
            this.format.setMaximumFractionDigits(section.getInt("decimal_settings.maximum_fraction"));

            this.suffixes.put("", 1.0);
            this.suffixes.put(section.getString("short_settings.thousand"), 1000.0);
            this.suffixes.put(section.getString("short_settings.million"), 1000000.0);
            this.suffixes.put(section.getString("short_settings.billion"), 1000000000.0);
            this.suffixes.put(section.getString("short_settings.trillion"), 1000000000000.0);
            this.suffixes.put(section.getString("short_settings.quadrillion"), 1000000000000000.0);
            this.suffixes.put(section.getString("short_settings.quintillion"), 1000000000000000000.0);
        }
    }

    public Double reverseFormat(String text) {
        text = text.toLowerCase();

        for (Map.Entry<String, Double> entry : this.suffixes.entrySet()) {
            String suffix = entry.getKey().toLowerCase();
            double multi = entry.getValue();

            if (text.endsWith(suffix))
                try {
                    return Double.parseDouble(text.replace(suffix, "")) * multi;
                } catch (NumberFormatException ignored) {
                }
        }

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

                int value = (int) Math.floor(Math.log10(number));
                int base = value / 3;

                if (base > 0 && base < this.suffixes.size()) {
                    double divisor = Math.pow(10, base * 3);
                    double shortNumber = number / divisor;

                    String suffix = this.suffixes.keySet().toArray(new String[0])[base];

                    DecimalFormat shortFormat = new DecimalFormat();
                    shortFormat.setRoundingMode(RoundingMode.HALF_UP);
                    shortFormat.setMinimumFractionDigits(0);
                    shortFormat.setMaximumFractionDigits(2);
                    shortFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));

                    return shortFormat.format(shortNumber) + suffix;
                } else
                    return number.toString();
            case "decimal":
                if (this.format == null)
                    return number.toString();

                return this.format.format(number);
            default:
                return number.toString();
        }
    }
}