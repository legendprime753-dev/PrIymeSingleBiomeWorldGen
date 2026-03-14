package de.priyme.singlebiome;

public record NoiseSettings(int baseY, int amplitude, double scale, int octaves, double persistence, int fillerDepth) {

    public NoiseSettings {
        amplitude = Math.max(0, Math.min(96, amplitude));
        scale = Math.max(1.0E-6D, Math.min(1.0D, scale));
        octaves = Math.max(1, Math.min(8, octaves));
        persistence = Math.max(0.05D, Math.min(1.0D, persistence));
        fillerDepth = Math.max(0, Math.min(8, fillerDepth));
    }
}
