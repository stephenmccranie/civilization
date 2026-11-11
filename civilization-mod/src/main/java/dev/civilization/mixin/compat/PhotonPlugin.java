package dev.civilization.mixin.compat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Optional client hook on Iris alone; never link Iris classes on the server or when absent. */
public final class PhotonPlugin implements IMixinConfigPlugin {
    // Methods each hook needs, as name and descriptor.
    private static final Map<String, Map<String, String>> SHAPES = Map.of(
            "net.irisshaders.iris.shaderpack.include.IncludeGraph", Map.of("readFile", "(Ljava/nio/file/Path;)Ljava/lang/String;"),
            "net.irisshaders.iris.gl.program.ProgramUniforms", Map.of("update", "()V"));
    public void onLoad(String name) {}
    public String getRefMapperConfig() { return null; }
    public boolean shouldApplyMixin(String target, String mixin) {
        var log = org.slf4j.LoggerFactory.getLogger("Civilization/Photon");
        var file = FMLLoader.getLoadingModList().getModFileById("iris");
        if (file == null) return false;
        if (file.getMods().stream().noneMatch(m -> m.getModId().equals("iris") && m.getVersion().toString().startsWith("1.8.14"))) {
            log.warn("Shared clouds and river waves inactive: unverified Iris version");
            return false;
        }
        try {
            var node = MixinService.getService().getBytecodeProvider().getClassNode(target);
            var wanted = SHAPES.get(target);
            if (wanted.entrySet().stream().allMatch(w -> node.methods.stream().anyMatch(m -> m.name.equals(w.getKey()) && m.desc.equals(w.getValue())))) return true;
        } catch (Exception ex) {
            log.warn("Cannot inspect Iris hook", ex);
        }
        log.warn("Shared clouds and river waves inactive: incompatible Iris hook signature in {}", target);
        return false;
    }
    public void acceptTargets(Set<String> mine, Set<String> others) {}
    public List<String> getMixins() { return null; }
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
