package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Short-lived player work, using normal event-wrapped placement and harvest paths. */
@EventBusSubscriber(modid="civilization")
public final class BuilderLineSystem {
    public static final int MAX_LENGTH=16;
    public static final double WORK_REACH=16;
    private static final Map<UUID,Job> JOBS=new HashMap<>();
    private static final Map<UUID,Integer> LAST_MARK=new HashMap<>();
    private static final class Job {
        final ServerLevel level; final BlockPos a; final Direction face; final boolean building; final int slot;
        ItemStack working; final ItemStack reel; List<BlockPos> cells=List.of(); List<BlockState> original=List.of();
        int index,wait; float progress;
        Job(ServerPlayer p,BlockPos a,Direction face){level=p.serverLevel();this.a=a;this.face=face;building=p.getMainHandItem().getItem() instanceof BlockItem;slot=p.getInventory().selected;working=p.getMainHandItem().copy();reel=p.getOffhandItem().copy();}
        boolean active(){return !cells.isEmpty();}
    }
    public static boolean equipped(Player p){return p.getOffhandItem().is(BuilderLineContent.LINE.get());}
    public static boolean supported(ItemStack s){
        if(s.getItem() instanceof BlockItem b)return plain(b.getBlock().defaultBlockState());
        return s.getItem() instanceof PickaxeItem || s.getItem() instanceof AxeItem || s.getItem() instanceof ShovelItem;
    }
    /** Deliberately excludes functional/custom machine blocks from the first pass. */
    public static boolean plain(BlockState s){
        var b=s.getBlock();var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b);
        return (id.getNamespace().equals("minecraft") || b==RoadContent.PAVERS.get())
            && !s.hasBlockEntity() && s.getFluidState().isEmpty() && !(b instanceof FallingBlock) && !(b instanceof TntBlock)
            && !(b instanceof LeavesBlock) && s.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO)>=0
            && s.isCollisionShapeFullBlock(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,BlockPos.ZERO);
    }
    public static List<BlockPos> line(BlockPos a,BlockPos b){
        int dx=b.getX()-a.getX(),dy=b.getY()-a.getY(),dz=b.getZ()-a.getZ();
        if((dx!=0?1:0)+(dy!=0?1:0)+(dz!=0?1:0)>1)return List.of();
        long length=Math.max(Math.abs((long)dx),Math.max(Math.abs((long)dy),Math.abs((long)dz)))+1;
        if(length>MAX_LENGTH)return List.of();
        var cells=new ArrayList<BlockPos>();for(int i=0;i<length;i++)cells.add(a.offset(Integer.signum(dx)*i,Integer.signum(dy)*i,Integer.signum(dz)*i));return List.copyOf(cells);
    }
    public static BlockPos target(Player p,BlockHitResult hit){
        return p.getMainHandItem().getItem() instanceof BlockItem ? new BlockPlaceContext(p,InteractionHand.MAIN_HAND,p.getMainHandItem(),hit).getClickedPos().immutable() : hit.getBlockPos().immutable();
    }
    public static boolean active(ServerPlayer p){var j=JOBS.get(p.getUUID());return j!=null&&j.active();}
    public static boolean selected(ServerPlayer p){return JOBS.containsKey(p.getUUID());}
    public static void mark(ServerPlayer p,boolean cancel){
        if(!equipped(p)||p.isSpectator()||!p.isAlive())return;
        if(cancel){finish(p,"Line canceled");return;}
        var last=LAST_MARK.get(p.getUUID());if(last!=null&&p.tickCount-last<4)return;LAST_MARK.put(p.getUUID(),p.tickCount);
        if(!supported(p.getMainHandItem())){message(p,"Hold full building blocks, a pickaxe, axe or shovel in your main hand");return;}
        EquipmentGrade.ensure(p.getMainHandItem(),p.serverLevel().random,p.serverLevel().registryAccess());
        var old=JOBS.get(p.getUUID());
        if(old!=null&&!matches(p,old)){finish(p,"Working item changed");old=null;}
        if(old!=null&&old.active()){message(p,"Line working — crouch-right-click to cancel");return;}
        var picked=p.pick(p.blockInteractionRange(),0,false);
        if(!(picked instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK){message(p,"Aim at a block face");return;}
        select(p,hit);
    }
    /** Also used by focused server tests; production hits are raycast on the server above. */
    static void select(ServerPlayer p,BlockHitResult hit){
        var pos=target(p,hit);var j=JOBS.get(p.getUUID());
        if(j==null){
            if(!allowed(p,pos)){message(p,"Cannot work at this endpoint");return;}
            j=new Job(p,pos,hit.getDirection());JOBS.put(p.getUUID(),j);sync(p,j);message(p,"Start marked — right-click the aligned end");return;
        }
        var cells=line(j.a,pos);if(cells.isEmpty()){message(p,"Align endpoints on one axis; maximum 16 blocks");return;}
        if(!matches(p,j)){finish(p,"Working item changed");return;}
        for(var cell:cells)if(!j.level.hasChunkAt(cell)){message(p,"Line crosses unloaded terrain");return;}
        j.cells=cells;j.original=cells.stream().map(j.level::getBlockState).toList();sync(p,j);
        step(p); // The second mark starts actual work immediately, with no held-input gate.
    }
    private static boolean matches(ServerPlayer p,Job j){
        return p.isAlive()&&!p.isSpectator()&&p.serverLevel()==j.level&&p.getInventory().selected==j.slot
            && ItemStack.isSameItemSameComponents(p.getOffhandItem(),j.reel)
            && ItemStack.isSameItemSameComponents(p.getMainHandItem(),j.working);
    }
    private static boolean allowed(ServerPlayer p,BlockPos pos){
        var l=p.serverLevel();return l.hasChunkAt(pos)&&!l.isOutsideBuildHeight(pos)&&l.getWorldBorder().isWithinBounds(pos)
            && l.mayInteract(p,pos)&&CivicAccess.allowed(l,pos,p)&&BoatSystem.at(l,pos)==null&&AirshipSystem.at(l,pos)==null;
    }
    private static boolean visible(ServerPlayer p,BlockPos pos){
        if(p.getEyePosition().distanceToSqr(pos.getCenter())>WORK_REACH*WORK_REACH)return false;
        var eye=p.getEyePosition();var end=pos.getCenter();
        // Check every traversed chunk before ClipContext can inspect terrain.
        int samples=(int)Math.ceil(eye.distanceTo(end)*2);for(int i=0;i<=samples;i++)if(!p.serverLevel().hasChunkAt(BlockPos.containing(eye.lerp(end,(double)i/Math.max(1,samples)))))return false;
        var hit=p.serverLevel().clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return hit.getType()==HitResult.Type.MISS||hit.getBlockPos().equals(pos);
    }
    static void step(ServerPlayer p){
        var j=JOBS.get(p.getUUID());if(j==null)return;
        if(!matches(p,j)){finish(p,"Working item changed or line canceled");return;}
        if(!j.active()){if(p.getEyePosition().distanceToSqr(j.a.getCenter())>WORK_REACH*WORK_REACH)finish(p,"Line out of reach");return;}
        var pos=j.cells.get(j.index);
        if(!allowed(p,pos)){finish(p,"Line stopped: protected, unloaded or unsupported location");return;}
        if(!visible(p,pos)){finish(p,"Line stopped: target out of reach or hidden");return;}
        var state=j.level.getBlockState(pos);
        if(!state.equals(j.original.get(j.index))){finish(p,"Line stopped: target changed");return;}
        if(j.wait>0){j.wait--;return;}
        if(j.building){
            if(!state.isAir()||!j.level.getFluidState(pos).isEmpty()){finish(p,"Line stopped: occupied cell");return;}
            var block=((BlockItem)p.getMainHandItem().getItem()).getBlock();
            var hit=new BlockHitResult(pos.getCenter(),j.face,pos,false);
            var result=p.getMainHandItem().useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,hit));
            if(!result.consumesAction()||!j.level.getBlockState(pos).is(block)){finish(p,"Line stopped: placement blocked");return;}
            j.wait=4;
        }else{
            if(!plain(state)||!state.canHarvestBlock(j.level,pos,p)){finish(p,"Line stopped: block needs a suitable tool");return;}
            if(j.progress==0){
                var e=net.neoforged.neoforge.common.CommonHooks.onLeftClickBlock(p,pos,j.face,net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
                if(e.isCanceled()||e.getUseItem().isFalse()){finish(p,"Line stopped: mining denied");return;}
                state.attack(j.level,pos,p);
            }
            j.progress+=p.isCreative()?1:state.getDestroyProgress(p,j.level,pos);
            j.level.destroyBlockProgress(p.getId(),pos,Math.min(9,(int)(j.progress*10)));
            if(j.progress<1)return;
            if(!p.gameMode.destroyBlock(pos)||j.level.getBlockState(pos).equals(state)){finish(p,"Line stopped: mining denied");return;}
            j.level.destroyBlockProgress(p.getId(),pos,-1);j.progress=0;j.wait=4;
        }
        j.index++;j.working=p.getMainHandItem().copy();p.swing(InteractionHand.MAIN_HAND,true);
        if(j.index==j.cells.size()){finish(p,"Line complete");return;}
        if(p.getMainHandItem().isEmpty()){finish(p,"Line stopped: blocks exhausted or tool broke");return;}
        sync(p,j);message(p,(j.building?"Building ":"Mining ")+j.index+" / "+j.cells.size());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        for(var id:new ArrayList<>(JOBS.keySet())){var p=e.getServer().getPlayerList().getPlayer(id);if(p==null)JOBS.remove(id);else step(p);}
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){finish(p,"Line canceled");LAST_MARK.remove(p.getUUID());}}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){JOBS.clear();LAST_MARK.clear();}
    // Vanilla use packets must not execute a second, ordinary block/item action.
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.HIGHEST) public static void use(PlayerInteractEvent.RightClickBlock e){if(equipped(e.getEntity())&&supported(e.getEntity().getMainHandItem())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    @SubscribeEvent public static void useAir(PlayerInteractEvent.RightClickItem e){if(equipped(e.getEntity())&&supported(e.getEntity().getMainHandItem())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}}
    private static void sync(ServerPlayer p,Job j){if(p.connection!=null)PacketDistributor.sendToPlayer(p,new BuilderLinePayload.State(j!=null,j==null?BlockPos.ZERO:j.a,j==null||!j.active()?BlockPos.ZERO:j.cells.getLast(),j!=null&&j.active(),j==null?0:j.index));}
    public static void finish(ServerPlayer p,String reason){var j=JOBS.remove(p.getUUID());if(j!=null&&j.active()&&j.index<j.cells.size())j.level.destroyBlockProgress(p.getId(),j.cells.get(j.index),-1);sync(p,null);message(p,reason);}
    private static void message(ServerPlayer p,String text){p.displayClientMessage(Component.literal(text),true);}
}
