package dev.civilization.client;

/** Render-thread state: hide terrain only after DH actually rendered this frame. */
public final class DhWarpTerrain {
    private static boolean rendered;
    private static long lastRenderTime;
    private static Object renderedLevel;
    private static long skippedDraws, skippedBuilds;
    public static void reset(){rendered=false;lastRenderTime=0;renderedLevel=null;}
    public static boolean wanted(){return DhTravelClient.view().multiplier()>1;}
    public static void begin(){rendered=false;}
    public static void rendered(){rendered=true;lastRenderTime=System.nanoTime();renderedLevel=net.minecraft.client.Minecraft.getInstance().level;}
    public static boolean hideTerrain(){return wanted()&&rendered;}
    // Mesh submission runs before DH drawing (and may follow a cancelled shadow pass).
    public static boolean deferMeshes(){return wanted()&&renderedLevel==net.minecraft.client.Minecraft.getInstance().level&&System.nanoTime()-lastRenderTime<250_000_000L;}
    public static void skipped(){skippedDraws++;}
    public static void skippedBuild(){skippedBuilds++;}
    public static long skippedBuilds(){return skippedBuilds;}
    public static long skippedDraws(){return skippedDraws;}
}
