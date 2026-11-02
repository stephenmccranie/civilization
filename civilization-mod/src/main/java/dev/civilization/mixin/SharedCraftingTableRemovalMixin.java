package dev.civilization.mixin;

import dev.civilization.SharedCraftingTables;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockBehaviour.class)
public abstract class SharedCraftingTableRemovalMixin {
    @Inject(method="onRemove",at=@At("HEAD"))
    private void civilization$dropInputs(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved,CallbackInfo ci){
        if(level instanceof ServerLevel server&&state.is(Blocks.CRAFTING_TABLE)&&!next.is(Blocks.CRAFTING_TABLE))
            SharedCraftingTables.get(server).removed(server,pos);
    }
}
