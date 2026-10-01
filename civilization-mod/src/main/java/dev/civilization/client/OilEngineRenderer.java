package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Native Blockbench wheel animation; stationary parts remain ordinary baked block models. */
@EventBusSubscriber(modid="civilization", value=Dist.CLIENT, bus=EventBusSubscriber.Bus.MOD)
public final class OilEngineRenderer implements BlockEntityRenderer<OilEngineEntity> {
    private final GeoBlockRenderer<WheelAnimation> renderer = new GeoBlockRenderer<>(new MachineGeoModel<>("oil_engine_wheel"));
    private final java.util.Map<OilEngineEntity, WheelAnimation> instances = new java.util.WeakHashMap<>();
    public OilEngineRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(OilEngineContent.ENTITY.get(), OilEngineRenderer::new);
    }
    @Override public AABB getRenderBoundingBox(OilEngineEntity entity) { return new AABB(entity.getBlockPos()).inflate(3); }
    @Override public void render(OilEngineEntity entity, float partial, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        if (entity.getLevel() == null) return;
        var wheelPos = entity.getBlockPos().relative(entity.front().getCounterClockWise()).relative(entity.front().getOpposite()).above();
        var state = entity.getLevel().getBlockState(wheelPos);
        if (!state.is(OilEngineContent.WHEEL.get()) || !state.getValue(EngineFlywheelBlock.LINKED)
                || state.getValue(CivicBlock.FACING) != entity.front()) return;
        WheelAnimation wheel = instances.computeIfAbsent(entity, WheelAnimation::new);
        wheel.setLevel(entity.getLevel()); wheel.setBlockState(entity.getBlockState()); wheel.running = entity.status == 5;
        renderer.render(wheel, partial, pose, buffer, light, overlay);
    }
    private static final class WheelAnimation extends BlockEntity implements GeoBlockEntity {
        private static final RawAnimation CYCLE = RawAnimation.begin().thenLoop("animation.oil_engine_wheel.cycle");
        private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
        private boolean running;
        WheelAnimation(OilEngineEntity entity) { super(OilEngineContent.ENTITY.get(), entity.getBlockPos(), entity.getBlockState()); }
        @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
        @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
            controllers.add(new AnimationController<WheelAnimation>(this,"work",0,state -> {
                state.setControllerSpeed(running ? 1 : 0);
                return state.setAndContinue(CYCLE);
            }));
        }
    }
}
