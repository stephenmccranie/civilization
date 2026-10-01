package dev.civilization;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Local display preferences; never changes the structure or server rules. */
public final class PreviewConfig {
    public enum Mode { TEXTURED, OUTLINE }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Mode> MODE;
    public static final ModConfigSpec.DoubleValue OPACITY;
    static {
        var builder = new ModConfigSpec.Builder();
        MODE = builder.comment("TEXTURED shows block textures; OUTLINE is a simpler compatibility fallback.")
                .defineEnum("previewMode", Mode.TEXTURED);
        OPACITY = builder.comment("Opacity per visible block face; overlapping blocks accumulate opacity.")
                .defineInRange("previewOpacity", 0.32, 0.05, 0.6);
        SPEC = builder.build();
    }
    private PreviewConfig() {}
}
