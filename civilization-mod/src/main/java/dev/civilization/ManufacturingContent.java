package dev.civilization;

import java.util.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

/** Small shared stock catalog. Existing finished goods keep their registry identities. */
public final class ManufacturingContent {
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(Civilization.MOD_ID);
    public static final DeferredItem<Item> HANDLE=part("tool_handle"), FASTENERS=part("iron_fasteners"), WHEEL=part("wooden_wheel");
    private static final Map<String,DeferredItem<Item>> PARTS=new LinkedHashMap<>();
    public static final List<String> SHAPES=List.of("pickaxe","axe","shovel","hoe","sword","saw");
    static {
        for(String metal:List.of("iron","copper","steel","gold"))for(String form:List.of("plate","rod"))PARTS.put(metal+"_"+form,part(metal+"_"+form));
        for(String metal:List.of("iron","gold","diamond"))for(String shape:SHAPES)if(!metal.equals("gold")||!shape.equals("saw"))PARTS.put(metal+"_"+shape+"_head",part(metal+"_"+shape+"_head"));
    }
    private static DeferredItem<Item> part(String name){return ITEMS.registerSimpleItem(name);}
    public static Item part(String metal,String form){return PARTS.get(metal+"_"+form).get();}
    public static Item head(String metal,String shape){return part(metal,shape+"_head");}
    public static int materialMultiplier(ItemStack stack){
        for(var e:PARTS.entrySet())if(stack.is(e.getValue().get()))return e.getKey().startsWith("diamond_")?4:e.getKey().startsWith("gold_")?2:1;
        return 1;
    }
    public static void register(IEventBus bus){ITEMS.register(bus);}
}
