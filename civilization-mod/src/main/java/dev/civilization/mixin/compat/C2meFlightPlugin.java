package dev.civilization.mixin.compat;

import java.util.*;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;
import org.spongepowered.asm.service.MixinService;

/** Both hooks are enabled together, only for the inspected C2ME release. */
public final class C2meFlightPlugin implements IMixinConfigPlugin {
    public void onLoad(String name){}
    public String getRefMapperConfig(){return null;}
    public boolean shouldApplyMixin(String target,String mixin){
        var log=org.slf4j.LoggerFactory.getLogger("Civilization/FlightChunks");
        var file=FMLLoader.getLoadingModList().getModFileById("c2me_notickvd");
        if(file==null)return false;
        if(file.getMods().stream().noneMatch(m->m.getModId().equals("c2me_notickvd")&&m.getVersion().toString().equals("0.4.0-alpha.0.122+1.21.1"))){log.warn("Directional loading inactive: unverified C2ME version");return false;}
        try {
            var loader=MixinService.getService().getBytecodeProvider().getClassNode("com.ishland.c2me.notickvd.common.PlayerNoTickLoader");
            var iterator=MixinService.getService().getBytecodeProvider().getClassNode("com.ishland.c2me.notickvd.common.iterators.SpiralIterator");
            boolean valid=has(loader,"lambda$new$0","(J)Lcom/ishland/c2me/notickvd/common/iterators/ChunkIterator;")
                &&loader.fields.stream().anyMatch(f->f.name.equals("tacs")&&f.desc.equals("Lnet/minecraft/server/level/ChunkMap;"))
                &&has(iterator,"next","()Lnet/minecraft/world/level/ChunkPos;")&&has(iterator,"hasNext","()Z")&&has(iterator,"remaining","()J")
                &&List.of("originX","originZ","radius").stream().allMatch(n->iterator.fields.stream().anyMatch(f->f.name.equals(n)&&f.desc.equals("I")));
            if(valid)log.info("C2ME directional flight loading enabled");else log.warn("Directional loading inactive: hook mismatch");
            return valid;
        }catch(Exception ex){log.warn("Directional loading inactive: cannot inspect hooks",ex);return false;}
    }
    private static boolean has(ClassNode n,String name,String desc){return n.methods.stream().anyMatch(m->m.name.equals(name)&&m.desc.equals(desc));}
    public void acceptTargets(Set<String> mine,Set<String> others){}
    public List<String> getMixins(){return null;}
    public void preApply(String target,ClassNode node,String mixin,IMixinInfo info){}
    public void postApply(String target,ClassNode node,String mixin,IMixinInfo info){}
}
