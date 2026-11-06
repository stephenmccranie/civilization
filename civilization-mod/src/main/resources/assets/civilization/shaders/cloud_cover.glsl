// Civilization's cloud field, the exact mirror of dev.civilization.CloudCover. Change both together.
uniform vec2 civilizationCloudShift;     // the camera's noise position, wrapped to the period
uniform vec2 civilizationCloudGrid;      // the camera's block offset from the district grid's west and north corner
uniform float civilizationCloudiness[9]; // three by three districts, west to east then north to south

uint civilization_hash(uint x) {
    x ^= x >> 16u;
    x *= 0x7feb352du;
    x ^= x >> 15u;
    x *= 0x846ca68bu;
    x ^= x >> 16u;
    return x;
}

float civilization_lattice(int x, int z, uint octave) {
    return float(civilization_hash(uint(x) * 0x8da6b343u ^ uint(z) * 0xd8163841u ^ octave * 0xcb1ab31fu) >> 8u) / 16777216.0;
}

float civilization_value(vec2 p, int size, uint octave) {
    float qx = p.x / float(size), qz = p.y / float(size);
    float ix = floor(qx), iz = floor(qz);
    float fx = qx - ix, fz = qz - iz;
    fx = fx * fx * (3.0 - 2.0 * fx);
    fz = fz * fz * (3.0 - 2.0 * fz);
    int cells = 65536 / size;
    int cx = (int(ix) + 4 * cells) % cells, cz = (int(iz) + 4 * cells) % cells;
    int ex = (cx + 1) % cells, ez = (cz + 1) % cells;
    float north = civilization_lattice(cx, cz, octave) * (1.0 - fx) + civilization_lattice(ex, cz, octave) * fx;
    float south = civilization_lattice(cx, ez, octave) * (1.0 - fx) + civilization_lattice(ex, ez, octave) * fx;
    return north * (1.0 - fz) + south * fz;
}

float civilization_cloudiness(vec2 g) {
    float u = clamp((g.x - 256.0) / 512.0, 0.0, 2.0), v = clamp((g.y - 256.0) / 512.0, 0.0, 2.0);
    int i = int(min(floor(u), 1.0)), j = int(min(floor(v), 1.0));
    float fu = u - float(i), fv = v - float(j);
    float north = civilizationCloudiness[j * 3 + i] * (1.0 - fu) + civilizationCloudiness[j * 3 + i + 1] * fu;
    float south = civilizationCloudiness[(j + 1) * 3 + i] * (1.0 - fu) + civilizationCloudiness[(j + 1) * 3 + i + 1] * fu;
    return north * (1.0 - fv) + south * fv;
}

// Cumulus density at a block offset from the camera, as Photon's own coverage would give it.
float civilization_cloud_density(vec2 offset) {
    vec2 p = civilizationCloudShift + offset;
    float noise = 0.55 * civilization_value(p, 128, 0u) + 0.25 * civilization_value(p, 64, 1u)
            + 0.15 * civilization_value(p, 32, 2u) + 0.05 * civilization_value(p, 16, 3u);
    float shape = 0.535 + 0.64 * (noise - 0.5), threshold = 0.71 - 0.5 * civilization_cloudiness(civilizationCloudGrid + offset);
    return clamp((shape - threshold) / (1.0 - threshold), 0.0, 1.0);
}
