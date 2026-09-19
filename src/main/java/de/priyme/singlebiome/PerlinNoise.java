package de.priyme.singlebiome;

import java.util.Random;

public final class PerlinNoise {

    private final int[] p;

    public PerlinNoise(long seed) {
        int[] permutation = new int[256];
        for (int i = 0; i < 256; i++) permutation[i] = i;

        Random rnd = new Random(seed);
        for (int i = 255; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
            int tmp = permutation[i];
            permutation[i] = permutation[index];
            permutation[index] = tmp;
        }

        p = new int[512];
        for (int i = 0; i < 512; i++) p[i] = permutation[i & 255];
    }

    public double noise(double x, double z) {
        int xi = fastFloor(x) & 255;
        int zi = fastFloor(z) & 255;

        double xf = x - fastFloor(x);
        double zf = z - fastFloor(z);

        double u = fade(xf);
        double v = fade(zf);

        int aa = p[p[xi] + zi];
        int ab = p[p[xi] + zi + 1];
        int ba = p[p[xi + 1] + zi];
        int bb = p[p[xi + 1] + zi + 1];

        double x1 = lerp(grad(aa, xf, zf), grad(ba, xf - 1, zf), u);
        double x2 = lerp(grad(ab, xf, zf - 1), grad(bb, xf - 1, zf - 1), u);

        return lerp(x1, x2, v);
    }

    public double octaveNoise(double x, double z, int octaves, double persistence) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;

        for (int i = 0; i < Math.max(1, octaves); i++) {
            total += noise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= 2;
        }
        return maxValue == 0 ? 0 : total / maxValue;
    }

    private int fastFloor(double x) {
        return x >= 0 ? (int) x : (int) x - 1;
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double lerp(double a, double b, double x) {
        return a + x * (b - a);
    }

    private double grad(int hash, double x, double z) {
        int h = hash & 15;
        double u = (h < 8) ? x : z;
        double v = (h < 4) ? z : (h == 12 || h == 14 ? x : 0);
        return (((h & 1) == 0) ? u : -u) + (((h & 2) == 0) ? v : -v);
    }
}
