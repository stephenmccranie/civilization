package dev.civilization;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

/** Existing slab item identities remain canonical; no duplicate slab recipe ecosystem. */
@EventBusSubscriber(modid="civilization")
public final class SlabIntegration {
    private static Map<Block,Block> bases, slabs;
    private static synchronized void initialize() {
        if(bases!=null)return;
        var forward=new HashMap<Block,Block>();var reverse=new HashMap<Block,Block>();
        BuiltInRegistries.BLOCK.stream().filter(b->b instanceof SlabBlock).sorted(Comparator.comparing(b->BuiltInRegistries.BLOCK.getKey(b).toString())).forEach(slab->{
            var id=BuiltInRegistries.BLOCK.getKey(slab);String path=id.getPath();
            if(!path.endsWith("_slab"))return;
            String stem=path.substring(0,path.length()-5);
            if(stem.equals("petrified_oak"))stem="oak";
            for(String candidate:List.of(stem,stem+"s",stem+"_block",stem+"_planks")) {
                var key=ResourceLocation.fromNamespaceAndPath(id.getNamespace(),candidate);
                if(!BuiltInRegistries.BLOCK.containsKey(key))continue;
                var block=BuiltInRegistries.BLOCK.get(key);
                if(!CuttingContent.cuttable(block.defaultBlockState()))continue;
                forward.put(slab,block);reverse.putIfAbsent(block,slab);break;
            }
        });
        bases=Map.copyOf(forward);slabs=Map.copyOf(reverse);
    }
    public static Block base(Block slab) {initialize();return bases.get(slab);}
    public static Block slab(Block base) {initialize();return slabs.get(base);}
    public static boolean isSlab(ItemStack stack) {return stack.getItem() instanceof BlockItem b && b.getBlock() instanceof SlabBlock && base(b.getBlock())!=null;}
    public static BlockState material(BlockState state) {var base=base(state.getBlock());return base==null?state:base.defaultBlockState();}
    @SubscribeEvent public static void place(UseItemOnBlockEvent event) {
        if(event.getUsePhase()!=UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK || !isSlab(event.getItemStack()))return;
        var result=CutPlacement.placeNativeSlab(new net.minecraft.world.item.context.BlockPlaceContext(event.getUseOnContext()));
        event.cancelWithResult(result.consumesAction() ? event.getLevel().isClientSide ? net.minecraft.world.ItemInteractionResult.SUCCESS : net.minecraft.world.ItemInteractionResult.CONSUME : net.minecraft.world.ItemInteractionResult.FAIL);
    }
}
