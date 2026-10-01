package dev.civilization;

import com.google.gson.*;
import java.util.*;
import java.io.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/** Shared material data. SI-shaped tuning values are not a conversion of legacy heat credits. */
@EventBusSubscriber(modid="civilization")
public final class PhysicalMaterials {
    public record Profile(String id,double density,double specificHeat,double conductivity,double legacyCapacity,double legacyConductance) {}
    private record Rules(Map<String,Profile> profiles,Map<ResourceLocation,String> blocks,Map<ResourceLocation,String> tags) {}
    private static volatile Rules rules=builtin();
    private PhysicalMaterials() {}
    private static Rules builtin(){
        try(var in=PhysicalMaterials.class.getResourceAsStream("/data/civilization/physical_materials/defaults.json")){
            if(in==null)throw new IllegalStateException("Missing physical material defaults");
            return parse(JsonParser.parseReader(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    static Rules parse(JsonObject root){
        var profiles=new HashMap<String,Profile>();
        root.getAsJsonObject("profiles").entrySet().forEach(e->{var o=e.getValue().getAsJsonObject();profiles.put(e.getKey(),new Profile(e.getKey(),positive(o,"density_kg_m3"),positive(o,"specific_heat_j_kg_k"),positive(o,"conductivity_w_m_k"),positive(o,"legacy_capacity"),positive(o,"legacy_conductance")));});
        for(var id:List.of("air","water","wood","masonry","glass","metal","door","partial"))if(!profiles.containsKey(id))throw new IllegalArgumentException("Missing profile "+id);
        return new Rules(Map.copyOf(profiles),selectors(root,"blocks",profiles),selectors(root,"tags",profiles));
    }
    private static double positive(JsonObject o,String key){double n=o.get(key).getAsDouble();if(!Double.isFinite(n)||n<1e-9||n>1e9)throw new IllegalArgumentException("Invalid material "+key);return n;}
    private static Map<ResourceLocation,String> selectors(JsonObject o,String key,Map<String,Profile> profiles){
        var result=new TreeMap<ResourceLocation,String>(Comparator.comparing(ResourceLocation::toString));
        o.getAsJsonObject(key).entrySet().forEach(e->{String id=e.getValue().getAsString();if(!profiles.containsKey(id))throw new IllegalArgumentException("Unknown material "+id);result.put(ResourceLocation.parse(e.getKey()),id);});
        return Collections.unmodifiableMap(result);
    }
    public static Profile named(String id){var p=rules.profiles.get(id);if(p==null)throw new IllegalArgumentException(id);return p;}
    public static Profile of(BlockState s){
        var r=rules;String exact=r.blocks.get(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(s.getBlock()));
        if(exact!=null)return r.profiles.get(exact);
        for(var e:r.tags.entrySet())if(s.is(TagKey.create(Registries.BLOCK,e.getKey())))return r.profiles.get(e.getValue());
        if(s.isAir())return r.profiles.get("air");
        if(!s.getFluidState().isEmpty()&&s.getBlock() instanceof LiquidBlock)return r.profiles.get("water");
        if(s.getBlock() instanceof DoorBlock||s.getBlock() instanceof TrapDoorBlock)return r.profiles.get("door");
        if(s.is(Blocks.GLASS)||s.is(Blocks.TINTED_GLASS)||s.getBlock() instanceof StainedGlassBlock||s.getBlock() instanceof StainedGlassPaneBlock||s.is(Blocks.GLASS_PANE))return r.profiles.get("glass");
        return r.profiles.get(s.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO)?"masonry":"partial");
    }
    /** Compatibility view preserves deployed heat balance until energy/capacity migration is approved. */
    public static double legacyConductance(BlockState s){
        if((s.getBlock() instanceof DoorBlock||s.getBlock() instanceof TrapDoorBlock)&&s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN))return 1;
        if(s.getBlock() instanceof DoorBlock||s.getBlock() instanceof TrapDoorBlock)return named("door").legacyConductance;
        if(s.is(Blocks.GLASS)||s.is(Blocks.TINTED_GLASS)||s.getBlock() instanceof StainedGlassBlock||s.getBlock() instanceof StainedGlassPaneBlock||s.is(Blocks.GLASS_PANE))return named("glass").legacyConductance;
        if(!s.getFluidState().isEmpty())return named("water").legacyConductance;
        return of(s).legacyConductance;
    }
    public static double legacyCapacity(BlockState s){
        if(s.isAir())return named("air").legacyCapacity;
        if(!s.getFluidState().isEmpty())return named("water").legacyCapacity;
        if(of(s).id.equals("wood"))return named("wood").legacyCapacity;
        return s.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE,net.minecraft.core.BlockPos.ZERO)?named("masonry").legacyCapacity:named("partial").legacyCapacity;
    }
    static void replace(JsonObject root){rules=parse(root);}
    @SubscribeEvent public static void reload(AddReloadListenerEvent e){e.addListener(new SimpleJsonResourceReloadListener(new Gson(),"physical_materials"){
        @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler){
            var value=input.get(ResourceLocation.parse("civilization:defaults"));
            if(value==null)throw new IllegalArgumentException("Missing civilization physical_materials/defaults.json");
            replace(value.getAsJsonObject()); // Atomic replacement; no cached state/tag resolutions to become stale.
        }
    });}
}
