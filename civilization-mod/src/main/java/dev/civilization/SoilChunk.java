package dev.civilization;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;
public final class SoilChunk implements INBTSerializable<CompoundTag> {
 private static final DeferredRegister<AttachmentType<?>> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES,"civilization");
 public static final DeferredHolder<AttachmentType<?>,AttachmentType<SoilChunk>> TYPE=TYPES.register("soil",()->AttachmentType.serializable(SoilChunk::new).build());
 public static void register(net.neoforged.bus.api.IEventBus bus){TYPES.register(bus);}
 public static final class Soil {public double water,work,target;public boolean fertilized,fruitMade,harvested;public String crop="";}
 public final Map<Long,Soil> soils=new HashMap<>();
 public final Map<Long,Integer> fruitYields=new HashMap<>();
 public int activeTicks;
 @Override public CompoundTag serializeNBT(HolderLookup.Provider lookup){var t=new CompoundTag();var list=new ListTag();soils.forEach((p,s)->{var e=new CompoundTag();e.putLong("p",p);e.putDouble("water",s.water);e.putDouble("work",s.work);e.putDouble("target",s.target);e.putBoolean("fertilized",s.fertilized);e.putBoolean("fruitMade",s.fruitMade);e.putBoolean("harvested",s.harvested);e.putString("crop",s.crop);list.add(e);});t.put("soil",list);var fruits=new ListTag();fruitYields.forEach((p,n)->{var e=new CompoundTag();e.putLong("p",p);e.putInt("n",n);fruits.add(e);});t.put("fruit",fruits);return t;}
 @Override public void deserializeNBT(HolderLookup.Provider lookup,CompoundTag t){soils.clear();fruitYields.clear();for(var n:t.getList("soil",10)){var e=(CompoundTag)n;var s=new Soil();s.water=Math.clamp(e.getDouble("water"),0,7200);s.work=Math.max(0,e.getDouble("work"));s.target=e.getDouble("target");s.fertilized=e.getBoolean("fertilized");s.fruitMade=e.getBoolean("fruitMade");s.harvested=e.getBoolean("harvested");s.crop=e.getString("crop");soils.put(e.getLong("p"),s);}for(var n:t.getList("fruit",10)){var e=(CompoundTag)n;fruitYields.put(e.getLong("p"),Math.clamp(e.getInt("n"),1,3));}}
}
