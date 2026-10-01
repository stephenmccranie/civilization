package dev.civilization;

public enum ClaimTier {
    HOMESTEAD(64, 1), ESTATE(128, 4), DISTRICT(256, 16);
    public final int width, upkeep;
    ClaimTier(int width, int upkeep) { this.width = width; this.upkeep = upkeep; }
    public int radius() { return width / 2; }
    public static ClaimTier step(int radius, boolean larger) {
        if (larger) { for (var tier : values()) if (tier.radius() > radius) return tier; return DISTRICT; }
        for (int i = values().length - 1; i >= 0; i--) if (values()[i].radius() < radius) return values()[i];
        return HOMESTEAD;
    }
}
