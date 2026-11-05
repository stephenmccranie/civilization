package dev.civilization;

/** Recipe tests start with a lit fire and fully heated masonry; ignition and warming have separate lifecycle coverage. */
final class CoalFireFixture {
    static void light(net.minecraft.world.level.block.entity.BlockEntity machine){
        var fire=machine instanceof KilnBlockEntity k?k.fire:machine instanceof WorkshopBlockEntity w?w.fire:machine instanceof IndustrialBlockEntity i&&i.coalPowered()?i.fire:null;
        if(fire!=null)set(fire,true,fire.capacity());
    }
    /** An unlit fire whose masonry holds `units` of work above working heat. */
    static void aboveWorking(CoalFire fire,double units){set(fire,false,CoalFire.WORKING*fire.capacity()+units*CoalFire.UNIT);}
    static double stored(CoalFire fire){var tag=new net.minecraft.nbt.CompoundTag();fire.save(tag);return tag.getCompound("coalFire").getDouble("stored");}
    static double reserve(CoalFire fire){var tag=new net.minecraft.nbt.CompoundTag();fire.save(tag);return tag.getCompound("coalFire").getDouble("reserve");}
    /** Sets the flame and stored heat, keeping the Coal already burning. */
    static void set(CoalFire fire,boolean lit,double stored){var tag=new net.minecraft.nbt.CompoundTag();fire.save(tag);var state=tag.getCompound("coalFire");state.putBoolean("lit",lit);state.putDouble("stored",stored);fire.load(tag);}
}
