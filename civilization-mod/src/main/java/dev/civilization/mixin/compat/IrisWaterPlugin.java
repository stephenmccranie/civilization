package dev.civilization.mixin.compat;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Optional client hook; never link Iris classes on the server or when absent. */
public final class IrisWaterPlugin implements IMixinConfigPlugin {
    public void onLoad(String name) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        var log = org.slf4j.LoggerFactory.getLogger("Civilization/IrisWater");
        var file = FMLLoader.getLoadingModList().getModFileById("iris");
        if (file == null) return false;
        if (file.getMods().stream().noneMatch(m -> m.getModId().equals("iris") && m.getVersion().toString().startsWith("1.8.14"))) {
            log.warn("Machine water shading inactive: unverified Iris version");
            return false;
        }
        try {
            var node = MixinService.getService().getBytecodeProvider().getClassNode(target);
            if (node.methods.stream().anyMatch(m -> m.name.equals("setBlockStateIds") && m.desc.equals("(Lit/unimi/dsi/fastutil/objects/Object2IntMap;)V"))) return true;
        } catch (Exception ex) {
            log.warn("Cannot inspect Iris water hook", ex);
        }
        log.warn("Machine water shading inactive: incompatible hook signature");
        return false;
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
