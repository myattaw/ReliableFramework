package me.rages.reliableframework.files;

import me.rages.reliableframework.files.messages.MessageConfig;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LanguageFile extends ConfigFile {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#(\\w{5}[0-9a-f])");
    private final Class<?> messageClass;

    public LanguageFile(JavaPlugin plugin, String fileName, Class<?> messageClass) {
        super(plugin, fileName);
        this.messageClass = messageClass;
    }

    @Override
    public LanguageFile init() {
        Arrays.stream(messageClass.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(MessageConfig.class))
                .forEach(field -> {
                    field.setAccessible(true);
                    MessageConfig annotation = field.getAnnotation(MessageConfig.class);
                    String configKey = annotation.config();
                    if (Modifier.isFinal(field.getModifiers()) || !Modifier.isPublic(field.getModifiers())
                            || !Modifier.isStatic(field.getModifiers())) {
                        // Log to console if field is not public and static
                        getPlugin().getLogger().log(
                                Level.WARNING,
                                String.format("%s must contain public and static modifiers only.", configKey)
                        );
                    } else {
                        try {
                            if (field.getType().equals(String.class)) {
                                String configValue = getConfig().getString(configKey);
                                if (configValue != null) {
                                    // Set the static field with the value from the config
                                    field.set(null, color(configValue));
                                } else {
                                    // Save the default value to the config
                                    getConfig().set(configKey, annotation.message()[0]);
                                }
                            } else if (field.getType().equals(List.class)) {
                                List<String> configValue = getConfig().getStringList(configKey);
                                if (configValue != null && !configValue.isEmpty()) {
                                    // Apply color transformation to each string in the list
                                    // Set the field with the colored list
                                    field.set(null, configValue.stream().map(this::color).collect(Collectors.toList()));
                                } else {
                                    // Save the default value to the config
                                    getConfig().set(configKey, Arrays.asList(annotation.message()));
                                }
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                });

        this.save();
        return this;
    }

    private String color(String textToTranslate) {
        Matcher matcher = HEX_PATTERN.matcher(textToTranslate);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + matcher.group(1)).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

}
