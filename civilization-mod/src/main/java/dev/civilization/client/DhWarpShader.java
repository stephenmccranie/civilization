package dev.civilization.client;

import java.util.regex.Pattern;

/** Match only Photon's named overdraw fade; leave other packs and other programs alone. */
public final class DhWarpShader {
    private static final Pattern FADE=Pattern.compile("smoothstep\\s*\\(\\s*dh_fade_start_distance\\s*,\\s*dh_fade_end_distance\\s*,\\s*view_distance\\s*\\)");
    private static final Pattern MAIN=Pattern.compile("void\\s+main\\s*\\(");
    public static long applied;
    private DhWarpShader(){}
    public static String patch(String source){
        if(source==null||!FADE.matcher(source).find()||!MAIN.matcher(source).find())return source;
        source=FADE.matcher(source).replaceAll("(civilizationWarpTerrain ? 1.0 : $0)");
        return MAIN.matcher(source).replaceFirst("uniform bool civilizationWarpTerrain;\n$0");
    }
}
