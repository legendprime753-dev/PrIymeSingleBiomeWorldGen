package de.priyme.singlebiome;

public record TreesSettings(boolean enabled, int perChunk, TreeStyle style) {

    public TreesSettings {
        int c = Math.max(0, Math.min(64, perChunk));
        perChunk = c;
        enabled = enabled && c > 0;
        style = style == null ? TreeStyle.VANILLA : style;
    }

    public static TreesSettings fromCount(int perChunk) {
        return fromCount(perChunk, TreeStyle.VANILLA);
    }

    public static TreesSettings fromCount(int perChunk, TreeStyle style) {
        int c = Math.max(0, Math.min(64, perChunk));
        return new TreesSettings(c > 0, c, style);
    }
}
