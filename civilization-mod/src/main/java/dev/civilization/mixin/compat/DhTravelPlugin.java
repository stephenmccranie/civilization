package dev.civilization.mixin.compat;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Optional, exact-version compatibility; no DH classes are linked by Civilization. */
public final class DhTravelPlugin implements IMixinConfigPlugin {
    public void onLoad(String name){}
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){
        var log=org.slf4j.LoggerFactory.getLogger("Civilization/DHTravel");
        var file=FMLLoader.getLoadingModList().getModFileById("distanthorizons");
        if(file==null){log.info("DH travel detail inactive: Distant Horizons absent");return false;}
        boolean version=file.getMods().stream().anyMatch(m->m.getModId().equals("distanthorizons")&&m.getVersion().toString().equals("3.3.1"));
        if(!version){log.warn("DH travel detail inactive: unverified Distant Horizons version");return false;}
        try {
            ClassNode node=MixinService.getService().getBytecodeProvider().getClassNode(target);
            if(mixin.endsWith(".IrisWarpFadeMixin")){
                var iris=FMLLoader.getLoadingModList().getModFileById("iris");
                return iris!=null&&iris.getMods().stream().anyMatch(m->m.getModId().equals("iris")&&m.getVersion().toString().startsWith("1.8.14"))
                    &&node.fields.stream().anyMatch(f->f.name.equals("id")&&f.desc.equals("I"))
                    &&node.methods.stream().anyMatch(m->m.name.equals("<init>")&&m.desc.equals("(Ljava/lang/String;ZZLnet/irisshaders/iris/gl/blending/BlendModeOverride;[Lnet/irisshaders/iris/gl/blending/BufferBlendOverride;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/irisshaders/iris/uniforms/custom/CustomUniforms;Lnet/irisshaders/iris/pipeline/IrisRenderingPipeline;)V"))
                    &&node.methods.stream().anyMatch(m->m.name.equals("fillUniformData")&&m.desc.equals("(Lorg/joml/Matrix4fc;Lorg/joml/Matrix4fc;IF)V"));
            }
            if(!mixin.endsWith(".DhTravelMixin")){
                var sodium=FMLLoader.getLoadingModList().getModFileById("sodium");
                if(sodium==null||sodium.getMods().stream().noneMatch(m->m.getModId().equals("sodium")&&m.getVersion().toString().startsWith("0.8.13"))){log.warn("Warp terrain suppression inactive: requires verified Sodium 0.8.13");return false;}
                String method=mixin.endsWith(".DhWarpClipMixin")?"getNearClipPlaneInBlocks":mixin.endsWith(".DhWarpRenderMixin")?"renderLodLayer":"renderLayer";
                String desc=mixin.endsWith(".DhWarpClipMixin")?"()F":mixin.endsWith(".DhWarpRenderMixin")?"(Z)V":"(Lnet/caffeinemc/mods/sodium/client/render/chunk/ChunkRenderMatrices;Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;DDD)V";
                boolean found=node.methods.stream().anyMatch(m->m.name.equals(method)&&m.desc.equals(desc));
                var renderer=MixinService.getService().getBytecodeProvider().getClassNode("com.seibel.distanthorizons.core.api.internal.ClientApi");
                var clip=MixinService.getService().getBytecodeProvider().getClassNode("com.seibel.distanthorizons.core.util.RenderUtil");
                var terrain=MixinService.getService().getBytecodeProvider().getClassNode("net.caffeinemc.mods.sodium.client.render.chunk.RenderSectionManager");
                found &= renderer.methods.stream().anyMatch(m->m.name.equals("renderLodLayer")&&m.desc.equals("(Z)V"))
                    &&clip.methods.stream().anyMatch(m->m.name.equals("getNearClipPlaneInBlocks")&&m.desc.equals("()F"))
                    &&terrain.methods.stream().anyMatch(m->m.name.equals("renderLayer")&&m.desc.endsWith(";DDD)V"))
                    &&terrain.methods.stream().anyMatch(m->m.name.equals("submitSectionTasks")&&m.desc.equals("(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/executor/ChunkJobCollector;Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/estimation/UploadResourceBudget;)V"));
                if(!found)log.warn("Warp terrain suppression hook unavailable: {}",target);
                return found;
            }
            boolean detail=node.methods.stream().anyMatch(m->m.name.equals("calcExpectedDetailLevel")&&m.desc.equals("(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;IID)B"));
            boolean tick=node.methods.stream().anyMatch(m->m.name.equals("updateAllRenderSections")&&m.desc.equals("(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;)V"));
            boolean handoff=node.methods.stream().anyMatch(m->m.name.equals("recursivelyUpdateRenderSectionNode")&&m.desc.equals("(Lcom/seibel/distanthorizons/core/pos/blockPos/DhBlockPos2D;Lcom/seibel/distanthorizons/core/util/objects/quadTree/QuadNode;Lcom/seibel/distanthorizons/core/util/objects/quadTree/QuadNode;Lcom/seibel/distanthorizons/core/util/objects/quadTree/QuadNode;J)Z"));
            if(detail&&tick&&handoff){log.info("DH 3.3.1 speed-adaptive detail enabled");return true;}
        }catch(Exception ex){log.warn("Cannot inspect DH travel hooks",ex);}
        log.warn("DH travel detail inactive: incompatible hook signatures");return false;
    }
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public List<String> getMixins(){return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
