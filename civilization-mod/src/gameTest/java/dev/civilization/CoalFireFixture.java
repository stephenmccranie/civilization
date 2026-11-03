package dev.civilization;

/** Existing recipe tests start with an already-lit fire; ignition has separate lifecycle coverage. */
final class CoalFireFixture {
    static void light(net.minecraft.world.level.block.entity.BlockEntity machine){
        var tag=new net.minecraft.nbt.CompoundTag();var state=new net.minecraft.nbt.CompoundTag();state.putBoolean("lit",true);tag.put("coalFire",state);
        if(machine instanceof KilnBlockEntity k)k.fire.load(tag);
        else if(machine instanceof WorkshopBlockEntity w)w.fire.load(tag);
        else if(machine instanceof IndustrialBlockEntity i&&i.coalPowered())i.fire.load(tag);
    }
}
