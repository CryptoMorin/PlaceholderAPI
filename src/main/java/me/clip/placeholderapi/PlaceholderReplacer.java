package me.clip.placeholderapi;

import com.google.common.collect.ImmutableSet;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;

import java.util.Set;

/**
 * This is certainly hard to understand, but it's fully optimized.
 */
public class PlaceholderReplacer {
    private static final Set<Character> COLOR_CODES = ImmutableSet.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'o', 'r');

    public static String evaluatePlaceholders(OfflinePlayer player, String str, Closure closure, boolean colorize) {
        StringBuilder builder = new StringBuilder(str.length());
        StringBuilder identifier = null;
        PlaceholderHook handler = null;
        boolean color = false;

        // Stages:
        //   Stage 0: No closures has been detected or the detected identifier is invalid. We're going forward appending normal string.
        //   Stage 1: The closure has been detected, looking for identifier...
        //   Stage 2: The identifier has been detected and the parameter has been found. Translating placeholder...
        int stage = 0;

        for (char ch : str.toCharArray()) {
            if (color && COLOR_CODES.contains(ch)) {
                builder.append(ChatColor.COLOR_CHAR).append(ch);
                color = false;
                continue;
            }

            // Check if the placeholder ends or starts.
            if (ch == closure.end || ch == closure.start) {
                // If the placeholder ends.
                if (stage == 2) {
                    String parameter = identifier.toString();
                    String translated = handler.onRequest(player, parameter);

                    if (translated == null) builder.append(identifier);
                    else builder.append(translated);

                    identifier = new StringBuilder();
                    stage = 0;
                    continue;
                } else if (stage == 1) { // If it just started | double closures. // If it's still hasn't verified the indentifier, reset.
                    builder.append(closure.start).append(identifier);
                }

                identifier = new StringBuilder();
                stage = 1;
                continue;
            }

            // Placeholder identifier started.
            if (stage == 1) {
                // Compare the current character with the idenfitier's.
                // We reached the end of our identifier.
                if (ch == '_') {
                    handler = PlaceholderAPI.PLACEHOLDERS.get(identifier.toString());
                    if (handler == null) {
                        builder.append(closure.start).append(identifier).append('_');
                        stage = 0;
                    } else {
                        identifier = new StringBuilder();
                        stage = 2;
                    }
                    continue;
                }

                // Keep building the identifier name.
                identifier.append(ch);
                continue;
            }

            // Building the placeholder parameter.
            if (stage == 2) {
                identifier.append(ch);
                continue;
            }

            // Nothing placeholder related was found.
            if (colorize && ch == '&') {
                color = true;
                continue;
            }
            builder.append(ch);
        }

        if (identifier != null) {
            if (stage > 0) builder.append(closure.end);
            builder.append(identifier);
        }
        return builder.toString();
    }

    public enum Closure {
        PERCENT('%', '%'), BRACKETS('[', ']'), BRACES('{', '}');

        public char start, end;

        Closure(char start, char end) {
            this.start = start;
            this.end = end;
        }
    }
}
