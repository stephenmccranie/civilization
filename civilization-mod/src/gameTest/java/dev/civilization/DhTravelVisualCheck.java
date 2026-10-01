package dev.civilization;

import dev.civilization.client.DhTravelClient;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

/** Inspect the actual transformed DH tree in the disposable flight world. */
final class DhTravelVisualCheck {
    private static long recoveryDraws, recoveryFadeUpdates;
    static void check(int ticks){
        if(ticks==1190){
            if(dev.civilization.client.DhWarpShader.applied!=recoveryFadeUpdates)throw new IllegalStateException("Photon fade override still active after recovery");
            if(net.neoforged.fml.ModList.get().isLoaded("distanthorizons")&&dev.civilization.client.DhWarpTerrain.skippedDraws()!=recoveryDraws)throw new IllegalStateException("Terrain still hidden after recovery");
            try {
                String log=java.nio.file.Files.readString(net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("logs/latest.log"));
                if(log.contains("Quad Tree tick exception"))throw new IllegalStateException("DH tree transition errors in flight log");
            }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
            return;
        }
        if(ticks!=270&&ticks!=725)return;
        if(!net.neoforged.fml.ModList.get().isLoaded("distanthorizons")){
            if(ticks==270)System.out.println("DH_TRAVEL_ABSENT_PASS");return;
        }
        try {
            double multiplier=DhTravelClient.view().multiplier();
            if(ticks==725){if(multiplier!=1)throw new IllegalStateException("Detail failed to recover: "+multiplier);recoveryDraws=dev.civilization.client.DhWarpTerrain.skippedDraws();recoveryFadeUpdates=dev.civilization.client.DhWarpShader.applied;System.out.println("DH_TRAVEL_RECOVERY_PASS");return;}
            if(dev.civilization.client.DhWarpTerrain.skippedDraws()==0||dev.civilization.client.DhWarpTerrain.skippedBuilds()==0)throw new IllegalStateException("Warp terrain suppression failed: draws="+dev.civilization.client.DhWarpTerrain.skippedDraws()+" builds="+dev.civilization.client.DhWarpTerrain.skippedBuilds());
            if(dev.civilization.client.DhWarpShader.applied==0)throw new IllegalStateException("Photon warp fade override never applied");
            System.out.println("DH_PHOTON_FADE_PASS updates="+dev.civilization.client.DhWarpShader.applied);
            float clip=(float)Class.forName("com.seibel.distanthorizons.core.util.RenderUtil").getMethod("getNearClipPlaneInBlocks").invoke(null);
            if(clip!=.5f)throw new IllegalStateException("DH near terrain clip not removed");
            System.out.println("DH_WARP_TERRAIN_PASS skipped="+dev.civilization.client.DhWarpTerrain.skippedDraws()+" deferredBuilds="+dev.civilization.client.DhWarpTerrain.skippedBuilds()+" clip="+clip);
            var mc=net.minecraft.client.Minecraft.getInstance();net.minecraft.client.Screenshot.grab(mc.gameDirectory,"airship-warp-terrain.png",mc.getMainRenderTarget(),m->{});
            if(multiplier!=DhTravelPolicy.WARP_MULTIPLIER)throw new IllegalStateException("Actual flight did not activate travel detail");
            Object world=Class.forName("com.seibel.distanthorizons.core.api.internal.SharedApi").getMethod("getAbstractDhWorld").invoke(null);
            for(Object level:(Iterable<?>)world.getClass().getMethod("getAllLoadedLevels").invoke(world)){
                Object module;try{module=level.getClass().getField("clientside").get(level);}catch(NoSuchFieldException ex){continue;}
                Object state=((AtomicReference<?>)module.getClass().getField("ClientRenderStateRef").get(module)).get();if(state==null)continue;
                Object tree=state.getClass().getField("quadtree").get(state);
                Class<?> pos=Class.forName("com.seibel.distanthorizons.core.pos.blockPos.DhBlockPos2D");Object origin=pos.getConstructor(int.class,int.class).newInstance(0,0);
                Method calc=tree.getClass().getMethod("calcExpectedDetailLevel",pos,int.class,int.class);
                Field policy=tree.getClass().getDeclaredField("civilization$travel");policy.setAccessible(true);
                Field lockField=tree.getClass().getDeclaredField("treeTickLock");lockField.setAccessible(true);Lock lock=(Lock)lockField.get(tree);lock.lock();
                Object before=policy.get(tree);
                try {
                    policy.set(tree,new DhTravelPolicy.View(1));byte near=(byte)calc.invoke(tree,origin,128,0),far=(byte)calc.invoke(tree,origin,1024,0);
                    policy.set(tree,new DhTravelPolicy.View(DhTravelPolicy.WARP_MULTIPLIER));byte nearFast=(byte)calc.invoke(tree,origin,128,0),farFast=(byte)calc.invoke(tree,origin,1024,0);
                    if(nearFast!=DhWarpBands.NEAR||farFast!=DhWarpBands.FAR||(byte)calc.invoke(tree,origin,400,0)!=DhWarpBands.NEAR||(byte)calc.invoke(tree,origin,4096,0)!=DhWarpBands.FAR)throw new IllegalStateException("DH detail selection mismatch");
                    System.out.println("DH_TRAVEL_HOOK_PASS multiplier="+multiplier+" near="+near+" fastNear="+nearFast+" far="+far+" fastFar="+farFast);return;
                }finally{policy.set(tree,before);lock.unlock();}
            }
            throw new IllegalStateException("No active DH rendering tree");
        }catch(ReflectiveOperationException ex){throw new IllegalStateException(ex);}
    }
}
