package dev.civilization.mixin.compat;

import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Optional client hook; never link Grassier Grass classes on the server or when absent. */
public final class GrassierGrassPlugin implements IMixinConfigPlugin {
    public void onLoad(String name) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        var log = org.slf4j.LoggerFactory.getLogger("Civilization/GrassierGrass");
        var file = FMLLoader.getLoadingModList().getModFileById("grassiergrass");
        if (file == null) return false;
        if (file.getMods().stream().noneMatch(m -> m.getModId().equals("grassiergrass")
                && m.getVersion().toString().equals("1.4.5"))) {
            log.warn("Grass snow tint override inactive: unverified Grassier Grass version");
            return false;
        }
        try {
            var node = MixinService.getService().getBytecodeProvider().getClassNode(target);
            if (node.methods.stream().anyMatch(m -> m.name.equals("snowBlend")
                    && m.desc.equals("(ZFFF)F") && (m.access & Opcodes.ACC_STATIC) != 0)) {
                log.info("Grassier Grass 1.4.5 automatic snow tint disabled");
                return true;
            }
        } catch (Exception ex) {
            log.warn("Cannot inspect Grassier Grass snow tint hook", ex);
        }
        log.warn("Grass snow tint override inactive: incompatible hook signature");
        return false;
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
