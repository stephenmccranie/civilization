// River waves that run downstream. The server sends the lines rivers run along; the client hands the chords
// nearest the camera to this program, each from its start to its end relative to the camera, with the length of
// river above each end and the river's half-width there. Chords of one line follow each other in order.
uniform int civilizationRiverCount;
uniform vec4 civilizationRiverLine[64];
uniform vec4 civilizationRiverAlong[64];

// The distance from p to chord k
float civilization_river_distance(int k, vec2 p, out float t) {
    vec4 c = civilizationRiverLine[k];
    vec2 ab = c.zw - c.xy;
    t = clamp(dot(p - c.xy, ab) / max(dot(ab, ab), 1e-6), 0.0, 1.0);
    return length(p - c.xy - ab * t);
}

// Chord k's direction, the length of river above p as chord k carries it on, and p's distance across it
void civilization_river_frame(int k, vec2 p, out vec2 dir, out float along, out float across) {
    vec4 c = civilizationRiverLine[k];
    vec4 a = civilizationRiverAlong[k];
    float len = max(length(c.zw - c.xy), 1e-3);
    dir = (c.zw - c.xy) / len;
    along = a.x + (a.y - a.x) * dot(p - c.xy, dir) / len;
    across = dir.x * (p.y - c.y) - dir.y * (p.x - c.x);
}

// On a river's surface, Photon's flowing water laid along the river: its waves run downstream, and its noise
// scrolls with them. Where p lies near the bend between two chords of a line, both are blended, so distance
// along the river and its direction carry on smoothly round the bend.
void civilization_river_flow(
    vec3 scene_pos,
    vec3 flat_normal,
    inout vec2 coord,
    inout vec2 flow_dir,
    inout bool flowing_water,
    inout mat3 tbn
) {
    if (flat_normal.y < 0.99 || civilizationRiverCount <= 0) return;
    vec2 p = scene_pos.xz;

    int k = 0;
    float dk = 1e9, tk = 0.0;
    for (int c = 0; c < civilizationRiverCount; ++c) {
        float t;
        float d = civilization_river_distance(c, p, t);
        if (d < dk) { dk = d; k = c; tk = t; }
    }
    vec4 widths = civilizationRiverAlong[k];
    float half_width = mix(widths.z, widths.w, tk);
    if (dk > 1.35 * half_width + 2.0) return;

    vec2 dir;
    float along, across;
    civilization_river_frame(k, p, dir, along, across);

    int n = tk < 0.5 ? k - 1 : k + 1;
    bool joined = n >= 0 && n < civilizationRiverCount && (n < k
        ? civilizationRiverLine[n].zw == civilizationRiverLine[k].xy
        : civilizationRiverLine[k].zw == civilizationRiverLine[n].xy);
    if (joined) {
        float tn;
        float dn = civilization_river_distance(n, p, tn);
        float w = 0.5 * (1.0 - smoothstep(0.0, max(half_width, 1.0), dn - dk));
        vec2 dir_n;
        float along_n, across_n;
        civilization_river_frame(n, p, dir_n, along_n, across_n);
        dir = normalize(mix(dir, dir_n, w));
        along = mix(along, along_n, w);
        across = mix(across, across_n, w);
    }

    // Photon reads a surface's coordinates as minus its position in the tangent frame, runs flowing waves towards
    // flow_dir and scrolls their noise towards -y: so y is minus the distance along, x the distance across.
    coord = vec2(across, -along);
    flow_dir = vec2(0.0, -1.0);
    flowing_water = true;
    tbn = mat3(vec3(dir.y, 0.0, -dir.x), vec3(dir.x, 0.0, dir.y), vec3(0.0, 1.0, 0.0));
}
