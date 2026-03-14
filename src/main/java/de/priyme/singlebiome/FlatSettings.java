package de.priyme.singlebiome;

public record FlatSettings(int surfaceY, int fillerDepth) {

    public FlatSettings {
        fillerDepth = Math.max(0, Math.min(8, fillerDepth));
    }
}
