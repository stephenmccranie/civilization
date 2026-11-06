package dev.civilization.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PhotonCloudsTest {
    private static final String COVERAGE="/shaders/include/sky/clouds/coverage_map.glsl";
    @Test void photonCoverageCallsTheSharedFieldBeforeItsCallers(){
        String source="const float clouds_coverage_map_distortion = 0.8;\n\nfloat clouds_cumulus_local_coverage(vec2 pos) {\n    return pos.x;\n}\n\nfloat render(vec2 uv) { return clouds_cumulus_local_coverage(uv); }\n";
        String patched=PhotonClouds.patch(COVERAGE,source);
        int field=patched.indexOf("float civilization_cloud_cover(vec2 offset)"),wrapper=patched.indexOf("float clouds_cumulus_local_coverage(vec2 pos) { return civilization_cloud_cover(pos / CLOUDS_SCALE); }");
        assertTrue(field>0&&wrapper>field&&wrapper<patched.indexOf("float render("),"Shared field and wrapper come before Photon's callers");
        assertTrue(patched.contains("float civilization_photon_local_coverage(vec2 pos) {\n    return pos.x;\n}"),"Photon's own coverage is kept under another name");
        assertTrue(patched.contains("uniform float civilizationCloudiness[9];"),"Cloudiness uniforms declared");
        assertTrue(PhotonClouds.patched.contains("coverage"));
    }
    @Test void groundShadowsComeFromTheCumulusLayerAlone(){
        String source="float cloud_shadow = bicubic_filter(cloud_shadow_map, cloud_shadow_pos).x;";
        assertEquals("float cloud_shadow = bicubic_filter(cloud_shadow_map, cloud_shadow_pos).y;",PhotonClouds.patch("C:/packs/photon/shaders/include/lighting/cloud_shadows.glsl",source));
        assertTrue(PhotonClouds.patched.contains("shadows"));
    }
    @Test void otherFilesAndUnrecognisedShapesPassUnchanged(){
        String other="float clouds_cumulus_local_coverage(vec2 pos) { return 1.0; }";
        assertSame(other,PhotonClouds.patch("/shaders/include/sky/clouds/cumulus.glsl",other));
        String changed="float clouds_cumulus_local_coverage(vec3 pos) { return 1.0; }";
        assertSame(changed,PhotonClouds.patch(COVERAGE,changed));
        assertNull(PhotonClouds.patch(COVERAGE,null));
    }
}
