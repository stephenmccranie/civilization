package dev.civilization.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DhWarpShaderTest {
    @Test void photonFadeBecomesRuntimeConditional(){
        String fade="smoothstep( dh_fade_start_distance,\n dh_fade_end_distance, view_distance )";
        String shader="#version 400\nvoid main() { float fade = "+fade+"; }";
        String patched=DhWarpShader.patch(shader);
        assertTrue(patched.startsWith("#version 400\n"));
        assertTrue(patched.contains("uniform bool civilizationWarpTerrain;\nvoid main()"));
        assertTrue(patched.contains("(civilizationWarpTerrain ? 1.0 : "+fade+")"));
    }
    @Test void unrelatedShadersUnchanged(){
        String source="#version 400\nvoid main(){float fade=smoothstep(0.0,1.0,2.0);}";
        assertSame(source,DhWarpShader.patch(source));assertNull(DhWarpShader.patch(null));
    }
}
