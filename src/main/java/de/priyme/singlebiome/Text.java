package de.priyme.singlebiome;

import org.bukkit.ChatColor;

public final class Text {

    private Text() {
    }

    public static String ok(String msg) {
        return ChatColor.GREEN + msg;
    }

    public static String error(String msg) {
        return ChatColor.RED + msg;
    }

    public static String info(String msg) {
        return ChatColor.YELLOW + msg;
    }
}
