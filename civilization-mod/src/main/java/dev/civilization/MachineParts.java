package dev.civilization;

import java.util.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** One invisible part block per machine model, registered under the id its definition names. */
public final class MachineParts {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("civilization");
    private static final Map<MachineModel,DeferredBlock<MachinePartBlock>> PARTS=new LinkedHashMap<>();
    static {
        for(var model:MachineModel.all())PARTS.put(model,BLOCKS.register(model.partBlock,()->{
            MachinePartBlock.PENDING.set(model);
            try{return new MachinePartBlock(BlockBehaviour.Properties.ofFullCopy(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(model.like))).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK));}
            finally{MachinePartBlock.PENDING.remove();}
        }));
    }
    private MachineParts() {}
    public static MachinePartBlock block(MachineModel model){return PARTS.get(model).get();}
    public static void register(IEventBus bus){BLOCKS.register(bus);}
}
