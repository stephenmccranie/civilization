package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.*;

public final class AirshipMenu extends AbstractContainerMenu {
    private final BlockPos anchor;
    private final ServerPlayer viewer;
    private List<BlockPos> inspected=List.of();
    private String message="Build clear of terrain, then inspect.";
    private long lastAction=Long.MIN_VALUE;
    public AirshipPayload.Snapshot snapshot;
    public AirshipMenu(int id,Inventory inv) { this(id,inv,null,BlockPos.ZERO); }
    private AirshipMenu(int id,Inventory inv,ServerPlayer player,BlockPos pos) { super(AirshipContent.MENU.get(),id);viewer=player;anchor=pos.immutable();if(player!=null&&AirshipSystem.at(player.serverLevel(),pos)!=null)message="Stop before disassembling. Blocks snap to the nearest cardinal direction in clear space."; }
    public static void open(ServerPlayer p,BlockPos pos) {
        if(!AirshipSystem.access(p,pos)){p.displayClientMessage(Component.literal("Airship prototype: Creative or operator access required."),true);return;}
        p.openMenu(new SimpleMenuProvider((id,inv,ignored)->new AirshipMenu(id,inv,p,pos),Component.literal("Airship Prototype")));
    }
    @Override public boolean stillValid(Player p) { return viewer==null||p==viewer&&AirshipSystem.access(viewer,anchor); }
    @Override public ItemStack quickMoveStack(Player p,int slot) { return ItemStack.EMPTY; }
    public void action(ServerPlayer p,int action,double power) {
        if(p!=viewer||p.containerMenu!=this||!stillValid(p))return;
        long now=p.serverLevel().getGameTime();if(lastAction!=Long.MIN_VALUE&&now-lastAction<4)return;lastAction=now;
        try {
            var ship=AirshipSystem.at(p.serverLevel(),anchor);
            switch(action) {
                case 0 -> { if(p.level().getBlockEntity(anchor) instanceof AirshipBlockEntity controller){controller.power(power);message="Power applied. No fuel is consumed.";} }
                case 1 -> { if(ship!=null)throw new IllegalStateException("Already assembled.");inspected=AirshipSystem.scan(p.serverLevel(),anchor,p);message=inspected.size()+" connected blocks. Assemble if this is your whole ship."; }
                case 2 -> {
                    if(ship!=null||inspected.isEmpty())throw new IllegalStateException("Inspect the structure first.");
                    var current=AirshipSystem.scan(p.serverLevel(),anchor,p);
                    if(!new HashSet<>(current).equals(new HashSet<>(inspected))){inspected=List.of();throw new IllegalStateException("Structure changed. Inspect again.");}
                    AirshipSystem.launch(p.serverLevel(),anchor,p,current);p.closeContainer();p.displayClientMessage(Component.literal("Airship assembled. Right-click its controller to pilot."),false);
                }
                case 3 -> { if(ship==null)throw new IllegalStateException("Assemble the ship first.");AirshipSystem.pilot(ship,p); }
                case 4 -> { if(ship==null)throw new IllegalStateException("The ship is already disassembled.");AirshipDisassembly.disassemble(ship,p);p.closeContainer();p.displayClientMessage(Component.literal("Airship disassembled. You can edit its blocks and assemble it again."),false); }
                default -> { return; }
            }
        } catch(IllegalArgumentException|IllegalStateException ex) { message=ex.getMessage(); }
        broadcastChanges();
    }
    @Override public void broadcastChanges() {
        super.broadcastChanges();if(viewer==null||viewer.containerMenu!=this||!stillValid(viewer))return;
        var l=viewer.serverLevel();var ship=AirshipSystem.at(l,anchor);
        double power=l.getBlockEntity(anchor) instanceof AirshipBlockEntity controller?controller.power():0;
        PacketDistributor.sendToPlayer(viewer,new AirshipPayload.Snapshot(containerId,power,ship!=null,ship==null?0:ship.getMassTracker().getMass(),AirshipSystem.speed(ship),message));
    }
}
