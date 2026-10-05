package com.redcodersgroup.bubbleshooter.bubble;

public enum BubbleColor {
    RED(0xFFFF1744, 0xFFFF5252, 0xFFC62828, 'R'),
    GREEN(0xFF00C853, 0xFF69F0AE, 0xFF1B5E20, 'G'),
    BLUE(0xFF0091EA, 0xFF40C4FF, 0xFF0D47A1, 'B'),
    YELLOW(0xFFFFD600, 0xFFFFFF00, 0xFFFF6F00, 'Y'),
    PURPLE(0xFFAA00FF, 0xFFE040FB, 0xFF4A148C, 'P'),
    ORANGE(0xFFFF6D00, 0xFFFFAB40, 0xFFBF360C, 'O'),
    CYAN(0xFF00E5FF, 0xFF84FFFF, 0xFF006064, 'C'),
    RAINBOW(0xFFFFFFFF, 0xFFFFFFFF, 0xFFD3D3D3, '*'),
    BOMB(0xFF212121, 0xFF616161, 0xFF000000, 'X'),
    LIGHTNING(0xFF3A3FE0, 0xFF7D89FF, 0xFF14126B, 'L'),
    FIREBALL(0xFFFF6A0A, 0xFFFFF5A0, 0xFF7A1206, 'F'),
    STONE(0xFF555B65, 0xFFC6CBD2, 0xFF1B1D22, 'S'),
    TRANSPARENT(0x40E0F7FA, 0x90FFFFFF, 0x6080DEEA, 'T'),
    NONE(0x00000000, 0x00000000, 0x00000000, '.');

    public final int primaryColor;
    public final int lightColor;
    public final int darkColor;
    public final char code;

    BubbleColor(int primaryColor, int lightColor, int darkColor, char code) {
        this.primaryColor = primaryColor;
        this.lightColor = lightColor;
        this.darkColor = darkColor;
        this.code = code;
    }

    public static java.util.List<BubbleColor> getPlayableColors() {
        return java.util.Arrays.asList(RED, GREEN, BLUE, YELLOW, PURPLE, ORANGE, CYAN);
    }

    public static BubbleColor fromChar(char c) {
        for (BubbleColor color : values()) {
            if (color.code == Character.toUpperCase(c)) {
                return color;
            }
        }
        return NONE;
    }
}
