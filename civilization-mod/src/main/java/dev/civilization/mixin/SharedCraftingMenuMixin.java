package dev.civilization.mixin;

import dev.civilization.SharedTables;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingMenu.class)
public abstract class SharedCraftingMenuMixin extends AbstractContainerMenu {
    @Shadow @Final private CraftingContainer craftSlots;
    protected SharedCraftingMenuMixin(MenuType<?> type,int id){super(type,id);}
    @Redirect(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at=@At(value="NEW",target="net/minecraft/world/inventory/TransientCraftingContainer"))
    private TransientCraftingContainer civilization$grid(AbstractContainerMenu menu,int width,int height,
            int id,Inventory inventory,ContainerLevelAccess access){
        var shared=SharedTables.open(access,SharedTables.Kind.CRAFTING,menu,inventory.player);
        return shared!=null?shared:new TransientCraftingContainer(menu,width,height);
    }
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("RETURN"))
    private void civilization$initial(int id,Inventory inventory,ContainerLevelAccess access,CallbackInfo ci){
        if(craftSlots instanceof SharedTables.Grid)slotsChanged(craftSlots);
    }
    @Inject(method="removed",at=@At("HEAD"),cancellable=true)
    private void civilization$keepInputs(Player player,CallbackInfo ci){
        if(craftSlots instanceof SharedTables.Grid grid){grid.close();super.removed(player);ci.cancel();}
    }
}
