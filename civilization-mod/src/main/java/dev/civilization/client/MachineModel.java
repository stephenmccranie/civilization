package dev.civilization.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Emitting light must not switch the entire controller casing to flat/full block lighting. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class MachineModel extends BakedModelWrapper<BakedModel> {
    private MachineModel(BakedModel original){super(original);}
    @Override public TriState useAmbientOcclusion(BlockState state,ModelData data,RenderType type) {
        return originalModel.useAmbientOcclusion()?TriState.TRUE:TriState.FALSE;
    }
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((id,model)->id.id().getNamespace().equals("civilization")
                && (java.util.Set.of("tannery","textile_workshop","smithy").contains(id.id().getPath()) || id.id().getPath().equals("foundry") || id.id().getPath().equals("brick_kiln") || id.id().getPath().equals("fertilizer_retort") || id.id().getPath().equals("cooking_station"))?new MachineModel(model):model);
    }
}
