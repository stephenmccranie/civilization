package dev.civilization;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** First prototype is a local hand-worked sizing screen, not a fuel generator. */
public final class CoalScreenEntity extends BlockEntity {
    public static final int CAPACITY=256;
    private int raw,coal;
    private long lastWork=Long.MIN_VALUE;
    public CoalScreenEntity(BlockPos p,BlockState s){super(CoalMiningContent.SCREEN_ENTITY.get(),p,s);}
    public int raw(){return raw;}
    public int coal(){return coal;}
    public int unload(CoalMinecart cart,Player p){
        if(!allowed(p)||cart.level()!=level||cart.position().distanceToSqr(worldPosition.getCenter())>6.25||cart.getDeltaMovement().lengthSqr()>.01||!CivicAccess.allowed(level,cart.blockPosition(),p))return 0;
        int n=cart.extract(CAPACITY-raw);raw+=n;changed();return n;
    }
    public boolean work(Player p){
        if(!allowed(p)||raw<4||coal>=64)return false;
        if(CalorieFoodData.active(p)&&CalorieFoodData.of(p).isDepleted())return false;
        long now=level.getGameTime();if(lastWork!=Long.MIN_VALUE&&now-lastWork<10)return false;lastWork=now;
        raw-=4;coal++;changed();if(!p.isCreative())CalorieFoodData.of(p).spendLabor(p,1,false,"coal_screening");
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GRAVEL_PLACE,net.minecraft.sounds.SoundSource.BLOCKS,.65f,.8f);return true;
    }
    public void use(Player p){
        if(!allowed(p)||!p.getMainHandItem().isEmpty())return;
        if(p.isShiftKeyDown()){
            var s=KilnContent.MINERAL_COAL.toStack(Math.min(32,coal));int offered=s.getCount();p.getInventory().add(s);coal-=offered-s.getCount();changed();
        }else {
            var carts=level.getEntitiesOfClass(CoalMinecart.class,new AABB(worldPosition).inflate(2));carts.sort(java.util.Comparator.comparingDouble(c->c.distanceToSqr(worldPosition.getCenter())));
            for(var cart:carts)if(unload(cart,p)>0)break;work(p);
        }
        p.displayClientMessage(Component.literal("Screen: "+raw+" raw units · "+coal+" Coal ready · crouch-click to collect"),true);
    }
    private boolean allowed(Player p){return level!=null&&!level.isClientSide&&p.level()==level&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(worldPosition.getCenter())<=20.25&&level.mayInteract(p,worldPosition)&&CivicAccess.allowed(level,worldPosition,p);}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void spill(){
        if(level instanceof net.minecraft.server.level.ServerLevel l){if(raw>0&&LooseCoalEntity.spawn(l,worldPosition.getCenter(),raw))raw=0;if(coal>0){net.minecraft.world.level.block.Block.popResource(l,worldPosition,KilnContent.MINERAL_COAL.toStack(coal));coal=0;}}
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putInt("rawCoal",raw);t.putInt("preparedCoal",coal);}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);raw=Math.clamp(t.getInt("rawCoal"),0,CAPACITY);coal=Math.clamp(t.getInt("preparedCoal"),0,64);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
