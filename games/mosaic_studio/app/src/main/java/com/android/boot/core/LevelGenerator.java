package com.android.boot.core;

import java.util.ArrayList;
import java.util.List;

public class LevelGenerator {
    private static final int[] PALETTE_SOFT = new int[] {0xFFE86A6A, 0xFFE9B04F, 0xFF62B36E, 0xFF5AA5E5};
    private static final int[] PALETTE_BRIGHT = new int[] {0xFF4B8CF5, 0xFFF25D5D, 0xFFFFC14D, 0xFF39C18C, 0xFF7F6BFF, 0xFF2B2D42};
    private static final int[] PALETTE_PASTEL = new int[] {0xFF9DC5FF, 0xFFFFC9C9, 0xFFFFE39A, 0xFFC6F4D2, 0xFFD5C7FF};
    private static final int[] PALETTE_MONO = new int[] {0xFF1E1F24, 0xFF4A4C55, 0xFF8B8E9A, 0xFFD8D9DE};

    public static int getLevelCount() {
        return 24;
    }

    public static Level getLevel(int index) {
        int size;
        if (index < 8) {
            size = 10;
        } else if (index < 16) {
            size = 12;
        } else {
            size = 16;
        }
        List<Level> levels = buildLevels(size);
        return levels.get(index % levels.size());
    }

    private static List<Level> buildLevels(int size) {
        List<Level> levels = new ArrayList<>();
        levels.add(makeIconLevel(size, PALETTE_SOFT, 0));
        levels.add(makeIconLevel(size, PALETTE_SOFT, 1));
        levels.add(makeSymmetryLevel(size, PALETTE_BRIGHT, true));
        levels.add(makeSymmetryLevel(size, PALETTE_BRIGHT, false));
        levels.add(makePaletteLimitLevel(size, PALETTE_PASTEL, 3));
        levels.add(makeNegativeSpaceLevel(size, PALETTE_PASTEL));
        levels.add(makeLineArtLevel(size, PALETTE_MONO));
        levels.add(makeGemLevel(size, PALETTE_BRIGHT));
        return levels;
    }

    private static Level makeIconLevel(int size, int[] palette, int variant) {
        int[] target = emptyTarget(size);
        int mid = size / 2;
        int color = 0;
        if (variant == 0) {
            setRect(target, size, mid - 2, 2, mid + 2, size - 3, color);
            setRect(target, size, 2, mid - 2, mid - 1, mid + 2, 1);
            setRect(target, size, mid + 1, mid - 2, size - 3, mid + 2, 2);
        } else {
            setRect(target, size, 2, 2, size - 3, size - 3, color);
            setRect(target, size, 4, 4, size - 5, size - 5, -1);
            setRect(target, size, 5, 5, size - 6, size - 6, 1);
        }
        return new Level(size, trimPalette(palette, 4), target, size * size, size * size, 0);
    }

    private static Level makeSymmetryLevel(int size, int[] palette, boolean horizontal) {
        int[] target = emptyTarget(size);
        int c0 = 0;
        int c1 = 1;
        int c2 = 2;
        for (int i = 0; i < size; i++) {
            int offset = (i % 3) + 1;
            setCell(target, size, i, offset, c0);
            setCell(target, size, i, size - offset - 1, c1);
        }
        int bandStart = size / 3;
        int bandEnd = size - bandStart - 1;
        for (int x = bandStart; x <= bandEnd; x++) {
            for (int y = bandStart; y <= bandEnd; y++) {
                if ((x + y) % 2 == 0) {
                    setCell(target, size, x, y, c2);
                }
            }
        }
        if (horizontal) {
            mirrorHorizontal(target, size);
        } else {
            mirrorVertical(target, size);
        }
        return new Level(size, trimPalette(palette, 4), target, size * size, size * size, 0);
    }

    private static Level makePaletteLimitLevel(int size, int[] palette, int colors) {
        int[] target = emptyTarget(size);
        int center = size / 2;
        for (int r = 0; r < size / 2; r++) {
            int c = r % colors;
            setRect(target, size, center - r, center - r, center + r, center + r, c);
        }
        return new Level(size, trimPalette(palette, colors), target, size * size / 2, size * size, 0);
    }

    private static Level makeNegativeSpaceLevel(int size, int[] palette) {
        int[] target = emptyTarget(size);
        int c = 0;
        setRect(target, size, 1, 1, size - 2, size - 2, c);
        setRect(target, size, 3, 3, size - 4, size - 4, -1);
        setRect(target, size, 5, 5, size - 6, size - 6, 1);
        return new Level(size, trimPalette(palette, 4), target, size * size, size * size, 0);
    }

    private static Level makeLineArtLevel(int size, int[] palette) {
        int[] target = emptyTarget(size);
        int c = 0;
        for (int i = 1; i < size - 1; i++) {
            setCell(target, size, i, i, c);
            setCell(target, size, size - i - 1, i, c);
        }
        setRect(target, size, size / 4, size / 4, size - size / 4 - 1, size - size / 4 - 1, 1);
        return new Level(size, trimPalette(palette, 4), target, size * size, size * size, 0);
    }

    private static Level makeGemLevel(int size, int[] palette) {
        int[] target = emptyTarget(size);
        int center = size / 2;
        for (int y = 0; y < size; y++) {
            int span = center - Math.abs(center - y);
            for (int x = center - span; x <= center + span; x++) {
                int c = (Math.abs(center - x) + Math.abs(center - y)) % 3;
                setCell(target, size, x, y, c);
            }
        }
        return new Level(size, trimPalette(palette, 5), target, size * size, size * size, 0);
    }

    private static int[] emptyTarget(int size) {
        int[] target = new int[size * size];
        for (int i = 0; i < target.length; i++) {
            target[i] = -1;
        }
        return target;
    }

    private static void setRect(int[] target, int size, int x0, int y0, int x1, int y1, int color) {
        int minX = Math.max(0, Math.min(x0, x1));
        int maxX = Math.min(size - 1, Math.max(x0, x1));
        int minY = Math.max(0, Math.min(y0, y1));
        int maxY = Math.min(size - 1, Math.max(y0, y1));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                setCell(target, size, x, y, color);
            }
        }
    }

    private static void setCell(int[] target, int size, int x, int y, int color) {
        if (x < 0 || y < 0 || x >= size || y >= size) {
            return;
        }
        target[y * size + x] = color;
    }

    private static void mirrorHorizontal(int[] target, int size) {
        for (int y = 0; y < size / 2; y++) {
            int mirrorY = size - y - 1;
            for (int x = 0; x < size; x++) {
                int a = target[y * size + x];
                int b = target[mirrorY * size + x];
                if (a != -1) {
                    target[mirrorY * size + x] = a;
                } else if (b != -1) {
                    target[y * size + x] = b;
                }
            }
        }
    }

    private static void mirrorVertical(int[] target, int size) {
        for (int x = 0; x < size / 2; x++) {
            int mirrorX = size - x - 1;
            for (int y = 0; y < size; y++) {
                int a = target[y * size + x];
                int b = target[y * size + mirrorX];
                if (a != -1) {
                    target[y * size + mirrorX] = a;
                } else if (b != -1) {
                    target[y * size + x] = b;
                }
            }
        }
    }

    private static int[] trimPalette(int[] palette, int count) {
        int[] trimmed = new int[count];
        System.arraycopy(palette, 0, trimmed, 0, count);
        return trimmed;
    }
}
