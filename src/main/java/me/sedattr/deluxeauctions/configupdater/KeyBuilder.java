package me.sedattr.deluxeauctions.configupdater;

import org.bukkit.configuration.file.FileConfiguration;

public class KeyBuilder {
    private final FileConfiguration config;
    private final char separator;
    private final StringBuilder builder;

    public KeyBuilder(FileConfiguration config, char separator) {
        this.config = config;
        this.separator = separator;
        this.builder = new StringBuilder();
    }

    public void parseLine(String line, boolean checkIfExists) {
        line = line.trim();

        String[] currentSplitLine = line.split(":");

        if (currentSplitLine.length > 2)
            currentSplitLine = line.split(": ");

        String key = currentSplitLine[0].replace("'", "").replace("\"", "");

        if (checkIfExists) {
            while (!builder.isEmpty() && !config.contains(builder.toString() + separator + key))
                removeLastKey();
        }

        if (!builder.isEmpty())
            builder.append(separator);

        builder.append(key);
    }

    public boolean isEmpty() {
        return builder.isEmpty();
    }
    public void clear() {
        builder.setLength(0);
    }

    public void removeLastKey() {
        if (builder.isEmpty())
            return;

        String keyString = builder.toString();
        String[] split = keyString.split("[" + separator + "]");
        int minIndex = Math.max(0, builder.length() - split[split.length - 1].length() - 1);
        builder.replace(minIndex, builder.length(), "");
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
