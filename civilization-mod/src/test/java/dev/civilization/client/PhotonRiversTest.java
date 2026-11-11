package dev.civilization.client;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PhotonRiversTest {
    private static final String PROGRAM="C:/packs/photon/shaders/program/gbuffers_all_translucent.fsh";
    private static final String SOURCE="uniform vec3 cameraPosition;\n\nvoid main() {\n#ifdef LOD_MOD_ACTIVE\n            mat3 tbn_fixed = tbn;\n#else\n#define tbn_fixed tbn\n#endif\n"
            +"            if (vertical_side) {\n                flow_dir = (vec3(0.0, 1.0, 0.0) * tbn_fixed).xy;\n            } else {\n"
            +"                flow_dir = flowing_water ? normalize(flat_normal.xz) : vec2(0.0);\n            }\n\n            normal_tangent = get_water_normal(world_pos, flat_normal, coord, flow_dir, light_levels.y, flowing_water);\n}\n";
    @Test void photonsWaterTakesTheRiverFrameBeforeItsWavesAreDrawn(){
        String patched=PhotonRivers.patch(PROGRAM,SOURCE);
        assertTrue(PhotonRivers.patched);
        int field=patched.indexOf("void civilization_river_flow("),main=patched.indexOf("void main() {"),call=patched.indexOf("civilization_river_flow(position_scene, flat_normal, coord, flow_dir, flowing_water, tbn_fixed);");
        assertTrue(field>0&&field<main,"The river field is declared before main");
        assertTrue(call>patched.indexOf("normalize(flat_normal.xz)")&&call<patched.indexOf("get_water_normal("),"The river frame replaces Photon's before the waves are drawn");
        assertTrue(patched.contains("#else\nmat3 tbn_fixed = tbn;\n#endif"),"The tangent frame is a variable the river can turn");
        assertFalse(patched.contains("#define tbn_fixed"));
        assertTrue(patched.contains("uniform vec4 civilizationRiverLine["+PhotonRivers.CHORDS+"];"),"Chord arrays match the client's");
    }
    @Test void otherFilesAndUnrecognisedShapesPassUnchanged(){
        assertSame(SOURCE,PhotonRivers.patch("C:/packs/photon/shaders/program/gbuffers_all_solid.fsh",SOURCE));
        String changed=SOURCE.replace("#define tbn_fixed tbn","#define tbn_fixed mat3(1.0)");
        assertSame(changed,PhotonRivers.patch(PROGRAM,changed));
        assertNull(PhotonRivers.patch(PROGRAM,null));
    }
    @Test void theNearestChordsStayInTheirLinesOrder(){
        // Chords along x from 0 to 100 blocks, one block each
        int n=100;float[] chords=new float[n*8];
        for(int k=0;k<n;k++){chords[k*8]=k;chords[k*8+2]=k+1;chords[k*8+5]=1;}
        int[] picked=PhotonRivers.nearest(chords,50.5,3);
        assertEquals(PhotonRivers.CHORDS,picked.length);
        for(int k=0;k<picked.length;k++)assertEquals(50-PhotonRivers.CHORDS/2+k,picked[k]);
    }
}
