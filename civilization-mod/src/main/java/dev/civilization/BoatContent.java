package dev.civilization;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
public final class BoatContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    public static final DeferredBlock<BoatHelmBlock> HELM=BLOCKS.register("boat_helm",()->new BoatHelmBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    static {ITEMS.registerSimpleBlockItem(HELM);}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
}
