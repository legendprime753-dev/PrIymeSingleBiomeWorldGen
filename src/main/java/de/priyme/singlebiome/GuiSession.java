package de.priyme.singlebiome;

import org.bukkit.block.Biome;

public final class GuiSession {

    private String worldName;
    private Biome biome;
    private GenerationMode mode;

    private boolean treesEnabled;
    private int treesPerChunk;
    private TreeStyle treeStyle;

    private Long seed;

    private String biomeSearch = "";
    private int biomePage = 0;

    private int worldPage = 0;

    private InputState inputState = InputState.NONE;

    public GuiSession(String worldName, Biome biome, GenerationMode mode, TreesSettings trees, Long seed) {
        this.worldName = worldName;
        this.biome = biome;
        this.mode = mode;
        this.treesEnabled = trees.enabled();
        this.treesPerChunk = trees.perChunk();
        this.treeStyle = trees.style();
        this.seed = seed;
    }

    public String worldName() {
        return worldName;
    }

    public void worldName(String worldName) {
        this.worldName = worldName;
    }

    public Biome biome() {
        return biome;
    }

    public void biome(Biome biome) {
        this.biome = biome;
    }

    public GenerationMode mode() {
        return mode;
    }

    public void mode(GenerationMode mode) {
        this.mode = mode;
    }

    public boolean treesEnabled() {
        return treesEnabled;
    }

    public void treesEnabled(boolean treesEnabled) {
        this.treesEnabled = treesEnabled;
        if (!treesEnabled) {
            this.treesPerChunk = 0;
        } else if (this.treesPerChunk <= 0) {
            this.treesPerChunk = 4;
        }
    }

    public int treesPerChunk() {
        return treesPerChunk;
    }

    public void treesPerChunk(int treesPerChunk) {
        int c = Math.max(0, Math.min(64, treesPerChunk));
        this.treesPerChunk = c;
        this.treesEnabled = c > 0;
    }

    public TreesSettings trees() {
        return new TreesSettings(treesEnabled, treesPerChunk, treeStyle);
    }

    public TreeStyle treeStyle() {
        return treeStyle;
    }

    public void treeStyle(TreeStyle treeStyle) {
        this.treeStyle = treeStyle == null ? TreeStyle.VANILLA : treeStyle;
    }

    public Long seed() {
        return seed;
    }

    public void seed(Long seed) {
        this.seed = seed;
    }

    public String biomeSearch() {
        return biomeSearch;
    }

    public void biomeSearch(String biomeSearch) {
        this.biomeSearch = biomeSearch == null ? "" : biomeSearch;
    }

    public int biomePage() {
        return biomePage;
    }

    public void biomePage(int biomePage) {
        this.biomePage = Math.max(0, biomePage);
    }

    public int worldPage() {
        return worldPage;
    }

    public void worldPage(int worldPage) {
        this.worldPage = Math.max(0, worldPage);
    }

    public InputState inputState() {
        return inputState;
    }

    public void inputState(InputState inputState) {
        this.inputState = inputState == null ? InputState.NONE : inputState;
    }

    public enum InputState {
        NONE,
        WORLD_NAME,
        BIOME_SEARCH,
        TREES_PER_CHUNK,
        SEED
    }
}
