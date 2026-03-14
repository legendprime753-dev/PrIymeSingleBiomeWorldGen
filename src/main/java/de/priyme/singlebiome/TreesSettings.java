package de.priyme.singlebiome;

public record TreesSettings(boolean enabled, int perChunk, TreeStyle style) {

    private static final int MAX_TREES_PER_CHUNK = 24;

    public TreesSettings {
        int c = Math.max(0, Math.min(MAX_TREES_PER_CHUNK, perChunk));
        perChunk = c;
        enabled = enabled && c > 0;
        style = style == null ? TreeStyle.VANILLA : style;
    }

    public static TreesSettings fromCount(int perChunk) {
        return fromCount(perChunk, TreeStyle.VANILLA);
    }

    public static TreesSettings fromCount(int perChunk, TreeStyle style) {
        int c = Math.max(0, Math.min(MAX_TREES_PER_CHUNK, perChunk));
        return new TreesSettings(c > 0, c, style);
    }
}
