package dev.civilization;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.client.MachineGeoModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Development-only integration example: no new gameplay block or production animation policy. */
@net.neoforged.fml.common.EventBusSubscriber(modid = "civilization", value = net.neoforged.api.distmarker.Dist.CLIENT,
        bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
public final class ModelProofRenderer implements BlockEntityRenderer<OilEngineEntity> {
    @net.neoforged.bus.api.SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.LOWEST)
    public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        if ("models".equals(System.getProperty("civilization.previewCheck")))
            event.registerBlockEntityRenderer(OilEngineContent.ENTITY.get(), context -> new ModelProofRenderer());
    }
    private final GeoBlockRenderer<ProofEntity> renderer = new GeoBlockRenderer<>(new MachineGeoModel<>("engine_proof"));
    private final java.util.Map<OilEngineEntity, ProofEntity> instances = new java.util.WeakHashMap<>();

    @Override public AABB getRenderBoundingBox(OilEngineEntity entity) { return new AABB(entity.getBlockPos()).inflate(3); }

    @Override public void render(OilEngineEntity entity, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        ProofEntity proof = instances.computeIfAbsent(entity, ProofEntity::new);
        proof.setLevel(entity.getLevel());
        proof.setBlockState(entity.getBlockState());
        proof.running = entity.status == 5;
        pose.pushPose();
        pose.translate(2, 0, 0);
        renderer.render(proof, partial, pose, buffer, light, overlay);
        pose.popPose();
    }

    private static final class ProofEntity extends BlockEntity implements GeoBlockEntity {
        private static final RawAnimation CYCLE = RawAnimation.begin().thenLoop("animation.engine_proof.cycle");
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private boolean running;

        private ProofEntity(OilEngineEntity entity) { super(OilEngineContent.ENTITY.get(), entity.getBlockPos(), entity.getBlockState()); }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<ProofEntity>(this, "work", 0, state -> {
                state.setControllerSpeed(running ? 1 : 0);
                return state.setAndContinue(CYCLE);
            }));
        }
    }
}
