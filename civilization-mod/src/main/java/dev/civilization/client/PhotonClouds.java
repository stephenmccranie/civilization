package dev.civilization.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Photon draws the server's cloud field: its cumulus coverage comes from {@code cloud_cover.glsl}, and ground cloud
 * shadows from that layer alone, so the cloud a player sees shading them is the one the server shades them with.
 * Other packs, and Photon files this does not recognise, pass through unchanged.
 */
public final class PhotonClouds {
    private static final Logger LOG = LoggerFactory.getLogger("Civilization/PhotonClouds");
    private static final Pattern COVERAGE = Pattern.compile("float\\s+clouds_cumulus_local_coverage\\s*\\(\\s*vec2\\s+pos\\s*\\)");
    private static final String SHADOW = "bicubic_filter(cloud_shadow_map, cloud_shadow_pos).x;";
    private static final String CUMULUS_SHADOW = "bicubic_filter(cloud_shadow_map, cloud_shadow_pos).y;";
    private static final String FIELD = read();
    /** Pack files rewritten so far; both are needed for Photon to draw the shared clouds. */
    public static final Set<String> patched = ConcurrentHashMap.newKeySet();

    private PhotonClouds() {}

    private static String read() {
        try (var in = PhotonClouds.class.getResourceAsStream("/assets/civilization/shaders/cloud_cover.glsl")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The source Iris reads for a pack file, rewritten if it is one of the two Photon files the clouds come from. */
    public static String patch(String path, String source) {
        if (source == null) return null;
        if (path.endsWith("/include/sky/clouds/coverage_map.glsl")) {
            var match = COVERAGE.matcher(source);
            if (!match.find()) return unrecognised(path, source);
            patched.add("coverage");
            // Photon's own coverage stays below under another name, so its callers further down see the shared field.
            return source.substring(0, match.start()) + FIELD
                    + "\nfloat clouds_cumulus_local_coverage(vec2 pos) { return civilization_cloud_cover(pos / CLOUDS_SCALE); }\n"
                    + "float civilization_photon_local_coverage(vec2 pos)" + source.substring(match.end());
        }
        if (path.endsWith("/include/lighting/cloud_shadows.glsl")) {
            if (!source.contains(SHADOW)) return unrecognised(path, source);
            patched.add("shadows");
            return source.replace(SHADOW, CUMULUS_SHADOW);
        }
        return source;
    }

    private static String unrecognised(String path, String source) {
        LOG.warn("Shared clouds inactive: {} does not have the expected Photon v1.3b shape; the pack draws its own clouds", path);
        return source;
    }
}
