package net.essentialsx.discord;

import com.earth2me.essentials.EssentialsConf;
import com.earth2me.essentials.IConf;
import com.earth2me.essentials.utils.FormatUtil;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordSettings implements IConf {
    private final EssentialsConf config;
    private final EssentialsDiscord plugin;

    private final Map<String, Long> nameToChannelIdMap = new HashMap<>();
    private final Map<Long, List<String>> channelIdToNamesMap = new HashMap<>();

    private OnlineStatus status;
    private Activity statusActivity;

    private MessageFormat consoleFormat;

    private MessageFormat discordToMcFormat;
    private MessageFormat tempMuteFormat;
    private MessageFormat tempMuteReasonFormat;
    private MessageFormat permMuteFormat;
    private MessageFormat permMuteReasonFormat;
    private MessageFormat unmuteFormat;
    private MessageFormat kickFormat;

    public DiscordSettings(EssentialsDiscord plugin) {
        this.plugin = plugin;
        this.config = new EssentialsConf(new File(plugin.getDataFolder(), "config.yml"));
        config.setTemplateName("/config.yml", EssentialsDiscord.class);
        reloadConfig();
    }

    public String getBotToken() {
        return config.getString("token", "");
    }

    public long getGuildId() {
        return config.getLong("guild", 0);
    }

    public long getPrimaryChannelId() {
        return config.getLong("channels.primary", 0);
    }

    public long getChannelId(String key) {
        try {
            return Long.parseLong(key);
        } catch (NumberFormatException ignored) {
            return nameToChannelIdMap.getOrDefault(key, 0L);
        }
    }

    public List<String> getKeysFromChannelId(long channelId) {
        return channelIdToNamesMap.get(channelId);
    }

    public String getMessageChannel(String key) {
        return config.getString("message-types." + key, "none");
    }

    public boolean isShowDiscordAttachments() {
        return config.getBoolean("show-discord-attachments", true);
    }

    public List<String> getPermittedFormattingRoles() {
        return config.getStringList("permit-formatting-roles");
    }

    public OnlineStatus getStatus() {
        return status;
    }

    public Activity getStatusActivity() {
        return statusActivity;
    }

    public boolean isAlwaysReceivePrimary() {
        return config.getBoolean("always-receive-primary", false);
    }

    public int getChatDiscordMaxLength() {
        return config.getInt("chat.discord-max-length", 2000);
    }

    public boolean isChatFilterNewlines() {
        return config.getBoolean("chat.filter-newlines", true);
    }

    public String getConsoleChannelDef() {
        return config.getString("console.channel", "none");
    }

    public MessageFormat getConsoleFormat() {
        return consoleFormat;
    }

    public String getConsoleWebhookName() {
        return config.getString("console.webhook-name", "EssX Console Relay");
    }

    public boolean isConsoleCommandRelay() {
        return config.getBoolean("console.command-relay", false);
    }

    public boolean isShowAvatar() {
        return config.getBoolean("show-avatar", false);
    }

    public boolean isShowName() {
        return config.getBoolean("show-name", false);
    }

    // General command settings

    public boolean isCommandEnabled(String command) {
        return config.getBoolean("commands." + command + ".enabled", true);
    }

    public boolean isCommandEphemeral(String command) {
        return config.getBoolean("commands." + command + ".hide-command", true);
    }

    public List<String> getCommandSnowflakes(String command) {
        return config.getStringList("commands." + command + ".allowed-roles");
    }

    public List<String> getCommandAdminSnowflakes(String command) {
        return config.getStringList("commands." + command + ".admin-roles");
    }

    // Message formats

    public MessageFormat getDiscordToMcFormat() {
        return discordToMcFormat;
    }

    public MessageFormat getMcToDiscordFormat(Player player) {
        final String format = getFormatString("mc-to-discord");
        final String filled;
        if (plugin.isPAPI()) {
            filled = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, format);
        } else {
            filled = format;
        }
        return generateMessageFormat(filled, "{displayname}: {message}", false,
                "username", "displayname", "message", "world", "prefix", "suffix");
    }

    public MessageFormat getTempMuteFormat() {
        return tempMuteFormat;
    }

    public MessageFormat getTempMuteReasonFormat() {
        return tempMuteReasonFormat;
    }

    public MessageFormat getPermMuteFormat() {
        return permMuteFormat;
    }

    public MessageFormat getPermMuteReasonFormat() {
        return permMuteReasonFormat;
    }

    public MessageFormat getUnmuteFormat() {
        return unmuteFormat;
    }

    public MessageFormat getJoinFormat(Player player) {
        final String format = getFormatString("join");
        final String filled;
        if (plugin.isPAPI()) {
            filled = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, format);
        } else {
            filled = format;
        }
        return generateMessageFormat(filled, ":exclamation: {displayname} has joined!", false,
                "username", "displayname", "defaultmessage");
    }

    public MessageFormat getQuitFormat(Player player) {
        final String format = getFormatString("quit");
        final String filled;
        if (plugin.isPAPI()) {
            filled = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, format);
        } else {
            filled = format;
        }
        return generateMessageFormat(filled, ":exclamation: {displayname} has left!", false,
                "username", "displayname", "defaultmessage");
    }

    public MessageFormat getDeathFormat(Player player) {
        final String format = getFormatString("death\"");
        final String filled;
        if (plugin.isPAPI()) {
            filled = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, format);
        } else {
            filled = format;
        }
        return generateMessageFormat(filled, ":skull: {displayname} has died!", false,
                "username", "displayname", "defaultmessage");
    }

    public MessageFormat getKickFormat() {
        return kickFormat;
    }

    private String getFormatString(String node) {
        final String pathPrefix = node.startsWith(".") ? "" : "messages.";
        return config.getString(pathPrefix + (pathPrefix.isEmpty() ? node.substring(1) : node));
    }

    private MessageFormat generateMessageFormat(String content, String defaultStr, boolean format, String... arguments) {
        content = content == null ? defaultStr : content;
        content = format ? FormatUtil.replaceFormat(content) : FormatUtil.stripFormat(content);
        for (int i = 0; i < arguments.length; i++) {
            content = content.replace("{" + arguments[i] + "}", "{" + i + "}");
        }
        return new MessageFormat(content);
    }

    @Override
    public void reloadConfig() {
        config.load();

        // Build channel maps
        nameToChannelIdMap.clear();
        channelIdToNamesMap.clear();
        final ConfigurationSection section = config.getConfigurationSection("channels");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (section.isLong(key)) {
                    final long value = section.getLong(key);
                    nameToChannelIdMap.put(key, value);
                    channelIdToNamesMap.computeIfAbsent(value, o -> new ArrayList<>()).add(key);
                }
            }
        }

        // Presence stuff
        status = OnlineStatus.fromKey(config.getString("presence.status", "online"));
        if (status == OnlineStatus.UNKNOWN) {
            // Default invalid status to online
            status = OnlineStatus.ONLINE;
        }

        //noinspection ConstantConditions
        final String activity = config.getString("presence.activity", "default").trim().toUpperCase().replace("CUSTOM_STATUS", "DEFAULT");
        statusActivity = null;
        Activity.ActivityType activityType = null;
        try {
            if (!activity.equals("NONE")) {
                activityType = Activity.ActivityType.valueOf(activity);
            }
        } catch (IllegalArgumentException e) {
            activityType = Activity.ActivityType.DEFAULT;
        }
        if (activityType != null) {
            //noinspection ConstantConditions
            statusActivity = Activity.of(activityType, config.getString("presence.message", "Minecraft"));
        }

        consoleFormat = generateMessageFormat(getFormatString(".console.format"), "[{timestamp} {level}] {message}", false,
                "timestamp", "level", "message");

        discordToMcFormat = generateMessageFormat(getFormatString("discord-to-mc"), "&6[#{channel}] &3{fullname}&7: &f{message}", true,
                "channel", "username", "tag", "fullname", "nickname", "color", "message");
        unmuteFormat = generateMessageFormat(getFormatString("unmute"), "{displayname} has been unmuted.", false, "username", "displayname");
        tempMuteFormat = generateMessageFormat(getFormatString("temporary-mute"), "{controllerdisplayname} muted {displayname} for {time}", false,
                "username", "displayname", "controllername", "controllerdisplayname", "time");
        permMuteFormat = generateMessageFormat(getFormatString("permanent-mute"), "{controllerdisplayname} permanently muted {displayname}", false,
                "username", "displayname", "controllername", "controllerdisplayname");
        tempMuteReasonFormat = generateMessageFormat(getFormatString("temporary-mute-reason"), "{controllerdisplayname} muted {displayname} for {time} with reason: {reason}", false,
                "username", "displayname", "controllername", "controllerdisplayname", "time", "reason");
        permMuteReasonFormat = generateMessageFormat(getFormatString("permanent-mute-reason"), "{controllerdisplayname} permanently muted {displayname} with reason: {reason}", false,
                "username", "displayname", "controllername", "controllerdisplayname", "reason");
        kickFormat = generateMessageFormat(getFormatString("kick"), "{displayname} was kicked with reason: {reason}", false,
                "username", "displayname", "reason");

        plugin.onReload();
    }
}
