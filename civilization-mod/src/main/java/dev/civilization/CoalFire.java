package dev.civilization;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.sounds.*;

/**
 * One server-owned fire per coal machine. Burning Coal heats the machine's masonry; work draws on that stored heat
 * while it is at working heat, and the masonry leaks into the air around it, faster in the cold. A lit fire replaces
 * what the work drew and warms the masonry on top, so the cost of working is mostly the warm-up. Every joule burned
 * reaches the heat field, leaked or with the work it did.
 */
public final class CoalFire {
    public static final int BUTTON=10000;
    /** A work unit of heat: a Coal's 250 MJ in its 400 units. */
    public static final double UNIT=Energy.COAL/ProductionEnergy.COAL;
    /** Work needs stored heat at this share of full. */
    public static final double WORKING=.8;
    /** Heat a block of masonry holds when full; any fire holds at least one Coal's worth. */
    static final double PER_MASONRY=.7*Energy.COAL;
    /** Unfed at neutral temperature, a full store leaks down to working heat in twenty minutes. */
    static final double LEAK_TICKS=20*60*20/Math.log(1/WORKING);
    /** Beyond replacing what work drew, the fire fills an empty store in two minutes. */
    static final double WARMING_TICKS=2*60*20;
    private final BlockEntity owner;
    private final CoalFireHost host;
    private boolean lit;
    private double reserve,stored,drawn,efficiency=1;
    private long nextStrike;
    public <T extends BlockEntity & CoalFireHost> CoalFire(T owner){this.owner=owner;this.host=owner;}
    public boolean lit(){return lit;}
    public double capacity(){return Math.max(Energy.COAL,host.masonry()*PER_MASONRY);}
    public boolean hot(){return stored>=WORKING*capacity();}
    /** Stored heat in thousandths of full, for menus. */
    public int heat(){return (int)Math.clamp(Math.round(1000*stored/capacity()),0,1000);}
    /** Whole minutes an unfed store stays at working heat in the present air. */
    public int minutesHot(){return hot()?(int)(LEAK_TICKS*efficiency*Math.log(stored/(WORKING*capacity()))/1200):0;}
    public int state(){return (lit?1:0)|(int)Math.clamp(nextStrike-owner.getLevel().getGameTime(),0,20)<<1|Math.min(minutesHot(),1<<20)<<6;}
    private boolean fuel(){for(int slot:host.coalFuelSlots())if(host.getItem(slot).is(KilnContent.MINERAL_COAL.get()))return true;return false;}
    /** Advances `ticks`: the masonry leaks into the air, and a lit fire tops it up from its fuel until both run out. */
    public void tick(int ticks){
        var level=owner.getLevel();
        if(level.getGameTime()%20<ticks)efficiency=ThermalField.efficiency(level,owner.getBlockPos());
        double leak=stored*ticks/(LEAK_TICKS*efficiency);
        stored-=leak;release(leak);
        if(lit){
            double want=Math.min(capacity()-stored,drawn+capacity()/WARMING_TICKS*ticks);
            while(want>0){
                if(reserve<=0&&!stoke()){lit=false;break;}
                double burned=Math.min(want,reserve);
                reserve-=burned;stored+=burned;want-=burned;
            }
        }
        drawn=0;owner.setChanged();
    }
    /** Takes `units` of work from stored heat; false while the masonry is below working heat. */
    public boolean draw(double units){
        if(!hot())return false;
        stored-=units*UNIT;drawn+=units*UNIT;release(units*UNIT);owner.setChanged();
        return true;
    }
    private boolean stoke(){
        for(int slot:host.coalFuelSlots())if(host.getItem(slot).is(KilnContent.MINERAL_COAL.get())){
            host.getItem(slot).shrink(1);reserve+=Energy.COAL;
            EnergyLog.machine(owner.getLevel(),owner.getBlockPos(),"coal_fire_fuel","civilization:mineral_coal",1,null,0);
            return true;
        }
        return false;
    }
    private void release(double joules){
        if(joules>0)ThermalField.fuel(owner.getLevel(),owner.getBlockPos(),joules*ThermalRules.COAL_WASTE_HEAT/Energy.COAL*host.coalWasteHeatFactor());
    }
    /** Whether rain reaches an open fire: it falls on the block in front of the controller, its work face. A roof over
     *  that block shelters the fire, and its chimney may stand outdoors. */
    public boolean rainedOut(){
        var level=owner.getLevel();var state=owner.getBlockState();var facing=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        if(level==null||!host.openFire()||!ThermalConfig.enabled()||!state.hasProperty(facing))return false;
        var face=owner.getBlockPos().relative(state.getValue(facing));
        return Chunks.loaded(level,face)&&level.isRainingAt(face);
    }
    public boolean strike(boolean structureReady){
        var level=owner.getLevel();if(level==null||level.isClientSide||lit||!structureReady||!fuel()&&reserve<=0||level.getGameTime()<nextStrike)return false;
        if(rainedOut())return false;
        nextStrike=level.getGameTime()+20;owner.setChanged();
        level.playSound(null,owner.getBlockPos(),SoundEvents.FLINTANDSTEEL_USE,SoundSource.BLOCKS,.65f,.9f+level.random.nextFloat()*.2f);
        if(level.random.nextInt(3)==0){lit=true;level.playSound(null,owner.getBlockPos(),SoundEvents.FIRECHARGE_USE,SoundSource.BLOCKS,.5f,.85f);}
        return true;
    }
    /** The click costs labor only after the server accepts it, even if the spark fails. */
    public boolean strike(Player player, boolean structureReady){
        if(!strike(structureReady))return false;
        CalorieFoodData.of(player).spendOther(player,CalorieConfig.IGNITE.get(),"machine_ignite",owner.getBlockPos().toShortString());
        return true;
    }
    /** A broken structure loses its fire and the heat in its masonry. */
    public void extinguish(){lit=false;reserve=0;stored=0;owner.setChanged();}
    public void save(CompoundTag tag){var t=new CompoundTag();t.putBoolean("lit",lit);t.putDouble("reserve",reserve);t.putDouble("stored",stored);t.putLong("strike",nextStrike);tag.put("coalFire",t);}
    public void load(CompoundTag tag){var t=tag.getCompound("coalFire");lit=t.getBoolean("lit");reserve=Math.max(0,t.getDouble("reserve"));stored=Math.max(0,t.getDouble("stored"));nextStrike=t.getLong("strike");}
}
