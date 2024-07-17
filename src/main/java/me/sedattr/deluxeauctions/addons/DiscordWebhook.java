package me.sedattr.deluxeauctions.addons;

import me.sedattr.deluxeauctions.DeluxeAuctions;
import me.sedattr.deluxeauctions.others.Logger;
import me.sedattr.deluxeauctions.others.PlaceholderUtil;
import me.sedattr.deluxeauctions.others.Utils;
import org.bukkit.configuration.ConfigurationSection;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhook {
    private final String url;

    public DiscordWebhook(String url) {
        this.url = url;
    }

    public void sendMessage(String type, PlaceholderUtil placeholderUtil) {
        if (type == null)
            return;

        ConfigurationSection section = DeluxeAuctions.getInstance().configFile.getConfigurationSection("addons.discord." + type);
        if (section == null)
            return;
        if (!section.getBoolean("enabled"))
            return;

        String message = section.getString("message");
        if (message == null || message.isEmpty())
            return;

        execute(Utils.replacePlaceholders(message, placeholderUtil));
    }

    public void execute(String content) {
        if (content == null)
            return;
        if (content.length() >= 2000)
            return;

        new Thread(() -> {
            try {
                final HttpsURLConnection connection = (HttpsURLConnection) new URL(this.url).openConnection();

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "DeluxeAuctions-DiscordWebhook");
                connection.setDoOutput(true);
                try (final OutputStream outputStream = connection.getOutputStream()) {
                    String preparedCommand = content.replaceAll("\\\\", "");
                    if (preparedCommand.endsWith(" *"))
                        preparedCommand = preparedCommand.substring(0, preparedCommand.length() - 2) + "*";

                    outputStream.write(("{\"content\":\"" + preparedCommand + "\"}").getBytes(StandardCharsets.UTF_8));
                }

                connection.getInputStream();
            } catch (final IOException e) {
                if (e.getMessage().contains("Server returned HTTP response code: 429"))
                    return;

                Logger.sendConsoleMessage("There is a problem in Discord Webhook!", Logger.LogLevel.ERROR);
                e.printStackTrace();
            }
        }).start();
    }
}