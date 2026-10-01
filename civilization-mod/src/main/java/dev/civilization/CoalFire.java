package dev.civilization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.*;

/** One server-owned fire per coal machine. Existing work budgets retain their native units. */
public final class CoalFire {
    public static final int BUTTON=10000;
    private final BlockEntity owner;
    private final CoalFireHost host;
    private boolean lit;
    private int idleRemainder;
    private int coalBudget;
    // Old saves already queued heat when coal was purchased. A missing value means
    // their existing reserve has no new emission left to account for.
    private double unspentWasteHeat;
    private long nextStrike;
    public <T extends BlockEntity & CoalFireHost> CoalFire(T owner){this.owner=owner;this.host=owner;}
    public boolean lit(){return lit;}
    /** Remaining fraction of the last purchased coal, in thousandths for menu synchronization. */
    public int remaining(){return lit?(int)Math.clamp(Math.round(1000.0*(heat()-idleRemainder/4000.0)/Math.max(1,coalBudget>0?coalBudget:capacity())),0,1000):0;}
    public int state(){return (lit?1:0)+((int)Math.clamp(nextStrike-owner.getLevel().getGameTime(),0,20)<<1);}
    private int heat(){return host.fireHeat();}
    private void heat(int value){host.fireHeat(value);owner.setChanged();}
    private int capacity(){return host.coalCapacity();}
    private int[] slots(){return host.coalFuelSlots();}
    private boolean fuel(){for(int slot:slots())if(host.getItem(slot).is(KilnContent.MINERAL_COAL.get()))return true;return false;}
    public boolean supply(int required){
        if(!lit)return false;
        while(heat()<required){
            int slot=-1;for(int i:slots())if(host.getItem(i).is(KilnContent.MINERAL_COAL.get())){slot=i;break;}
            if(slot<0)return false;
            host.getItem(slot).shrink(1);
            int budget=host.coalBudget();
            coalBudget=budget;
            heat(heat()+budget);
            unspentWasteHeat+=ThermalRules.COAL_WASTE_HEAT;
            EnergyLog.machine(owner.getLevel(),owner.getBlockPos(),"coal_fire_fuel","civilization:mineral_coal",1,null,0);
        }return true;
    }
    /** Release this reserve's waste heat at the rate the fire actually spends it. */
    public void spent(int units){
        if(units<=0||unspentWasteHeat<=0)return;
        int reserve=heat()+units;
        if(reserve<=0)return;
        double released=unspentWasteHeat*Math.min(units,reserve)/(double)reserve;
        unspentWasteHeat=Math.max(0,unspentWasteHeat-released);
        // Apply at spending time so already-purchased saved reserves use current tuning.
        ThermalField.fuel(owner.getLevel(),owner.getBlockPos(),released*host.coalWasteHeatFactor());
        owner.setChanged();
    }
    public boolean strike(boolean structureReady){
        var level=owner.getLevel();if(level==null||level.isClientSide||lit||!structureReady||(!fuel()&&heat()==0)||level.getGameTime()<nextStrike)return false;
        if(!host.coalFireIgnoresRain()&&MachineWeather.wetWorkFace(level,owner.getBlockPos(),owner.getBlockState()))return false;
        nextStrike=level.getGameTime()+20;owner.setChanged();
        level.playSound(null,owner.getBlockPos(),SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,.65f,.9f+level.random.nextFloat()*.2f);
        if(level.random.nextInt(3)==0){lit=true;supply(1);level.playSound(null,owner.getBlockPos(),SoundEvents.FIRECHARGE_USE,SoundSource.BLOCKS,.5f,.85f);}
        return true;
    }
    /** The click costs labor only after the server accepts it, even if the spark fails. */
    public boolean strike(Player player, boolean structureReady){
        if(!strike(structureReady))return false;
        CalorieFoodData.of(player).spendOther(player,CalorieConfig.IGNITE.get(),"machine_ignite",owner.getBlockPos().toShortString());
        return true;
    }
    public void finish(int ticks,boolean working){
        if(!lit)return;
        if(!working){idleRemainder+=ticks*capacity();int cost=idleRemainder/4000;idleRemainder%=4000;if(cost>0){supply(cost);int spent=Math.min(heat(),cost);heat(heat()-spent);spent(spent);}}
        if(heat()==0&&!fuel())extinguish();
        owner.setChanged();
    }
    public void extinguish(){lit=false;idleRemainder=0;unspentWasteHeat=0;heat(0);owner.setChanged();}
    public void save(CompoundTag tag){var t=new CompoundTag();t.putBoolean("lit",lit);t.putInt("idle",idleRemainder);t.putInt("budget",coalBudget);t.putDouble("unspentWasteHeat",unspentWasteHeat);t.putLong("strike",nextStrike);tag.put("coalFire",t);}
    public void load(CompoundTag tag){var t=tag.getCompound("coalFire");lit=t.getBoolean("lit");coalBudget=Math.max(0,t.getInt("budget"));unspentWasteHeat=Math.max(0,t.getDouble("unspentWasteHeat"));idleRemainder=Math.clamp(t.getInt("idle"),0,3999);nextStrike=t.getLong("strike");}
}
