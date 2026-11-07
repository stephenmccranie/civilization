package dev.civilization.mixin;

import dev.civilization.SharedTables;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** A placed enchanting table keeps its item and lapis for every viewer, as a crafting table keeps its inputs. */
@Mixin(EnchantmentMenu.class)
public abstract class SharedEnchantingMenuMixin extends AbstractContainerMenu {
    @Shadow @Final @Mutable private Container enchantSlots;
    protected SharedEnchantingMenuMixin(MenuType<?> type,int id){super(type,id);}
    @Redirect(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at=@At(value="FIELD",target="Lnet/minecraft/world/inventory/EnchantmentMenu;enchantSlots:Lnet/minecraft/world/Container;",opcode=Opcodes.PUTFIELD))
    private void civilization$slots(EnchantmentMenu menu,Container own,int id,Inventory inventory,ContainerLevelAccess access){
        var shared=SharedTables.open(access,SharedTables.Kind.ENCHANTING,menu,inventory.player);
        enchantSlots=shared!=null?shared:own;
    }
    @Inject(method="<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",at=@At("RETURN"))
    private void civilization$initial(int id,Inventory inventory,ContainerLevelAccess access,CallbackInfo ci){
        if(enchantSlots instanceof SharedTables.Grid)slotsChanged(enchantSlots);
    }
    /** An enchantment choice spends the shared lapis, so access is resolved at the click, as for slot clicks. */
    @Inject(method="clickMenuButton",at=@At("HEAD"),cancellable=true)
    private void civilization$access(Player player,int id,CallbackInfoReturnable<Boolean> result){
        if(enchantSlots instanceof SharedTables.Grid grid&&!grid.stillValid(player)){player.closeContainer();result.setReturnValue(false);}
    }
    @Inject(method="removed",at=@At("HEAD"),cancellable=true)
    private void civilization$keepSlots(Player player,CallbackInfo ci){
        if(enchantSlots instanceof SharedTables.Grid grid){grid.close();super.removed(player);ci.cancel();}
    }
}
