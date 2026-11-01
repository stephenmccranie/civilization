package dev.civilization;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import static dev.civilization.WorkshopJobs.*;

/** One authoritative catalog for powered forming and hand joining; no recursive recipe execution. */
public final class ManufacturingJobs {
    private static List<Job> forging,assembly;
    public static final int BENCH=3;
    private static Need planks(int n){return new Need(Ingredient.of(ItemTags.PLANKS),n);}
    private static Need part(String metal,String form,int n){return need(ManufacturingContent.part(metal,form),n);}
    private static void add(List<Job> jobs,Item output,int count,int ticks,Need... input){
        var needs=new ArrayList<>(List.of(input));while(needs.size()<4)needs.add(empty());
        if(needs.size()!=4)throw new IllegalArgumentException("At most four ingredient types: "+output);
        jobs.add(new Job(output.getDescriptionId(),new ItemStack(output,count),List.copyOf(needs),ticks));
    }
    private static Item item(String id){return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));}
    public static String metal(Item material){return material==Items.DIAMOND?"diamond":material==Items.GOLD_INGOT?"gold":"iron";}
    public static String shape(Item output){
        String id=BuiltInRegistries.ITEM.getKey(output).getPath();
        if(id.contains("saw"))return "saw";
        if(id.contains("pick"))return "pickaxe";
        for(String s:ManufacturingContent.SHAPES)if(id.endsWith(s))return s;
        return null;
    }
    public static synchronized List<Job> forging(){
        if(forging!=null)return forging;
        var jobs=new ArrayList<Job>();
        for(String metal:List.of("iron","copper","steel","gold")){
            Item ingot=metal.equals("steel")?KilnContent.STEEL.get():item("minecraft:"+metal+"_ingot");
            add(jobs,ManufacturingContent.part(metal,"plate"),1,100,need(ingot,1));
            add(jobs,ManufacturingContent.part(metal,"rod"),2,100,need(ingot,1));
        }
        add(jobs,ManufacturingContent.FASTENERS.get(),8,100,need(Items.IRON_INGOT,1));
        int[] amounts={3,3,1,2,2,3};
        for(String metal:List.of("iron","gold","diamond"))for(int i=0;i<ManufacturingContent.SHAPES.size();i++)if(!metal.equals("gold")||i!=5)
            add(jobs,ManufacturingContent.head(metal,ManufacturingContent.SHAPES.get(i)),1,500,need(metal.equals("diamond")?Items.DIAMOND:metal.equals("gold")?Items.GOLD_INGOT:Items.IRON_INGOT,amounts[i]));
        forging=List.copyOf(jobs);return forging;
    }
    public static synchronized List<Job> assembly(){
        if(assembly!=null)return assembly;
        var jobs=new ArrayList<Job>();jobs.add(new Job("Repair",ItemStack.EMPTY,List.of(),200));
        add(jobs,ManufacturingContent.HANDLE.get(),4,100,planks(1),need(Items.LEATHER,1));
        add(jobs,ManufacturingContent.WHEEL.get(),1,200,planks(1),need(ManufacturingContent.FASTENERS.get(),2));
        for(var old:WorkshopJobs.equipmentJobs()){
            if(old.output().isEmpty())continue;
            Item output=old.output().getItem();var e=WorkshopJobs.equipment(old.output());
            if(e==null){
                var needs=old.needs().stream().map(n->replaceIngots(n)).toList();
                jobs.add(new Job(old.name(),old.output().copy(),needs,200));continue;
            }
            if(e.material()==Items.NETHERITE_INGOT){jobs.add(new Job(old.name(),old.output().copy(),old.needs(),200));continue;}
            String shape=shape(output);
            if(shape!=null)add(jobs,output,1,100,need(ManufacturingContent.head(metal(e.material()),shape),1),need(ManufacturingContent.HANDLE.get(),1));
            else if(e.armor())add(jobs,output,1,200,need(e.material()==Items.DIAMOND?Items.DIAMOND:ManufacturingContent.part(metal(e.material()),"plate"),e.amount()),need(Items.LEATHER,1),need(WorkshopContent.CLOTH.get(),2));
            else if(output==Items.SHEARS)add(jobs,output,1,100,part("iron","plate",2),need(ManufacturingContent.FASTENERS.get(),1));
            else if(output==Items.SHIELD)add(jobs,output,1,200,planks(6),part("iron","plate",1),need(Items.LEATHER,1));
        }
        add(jobs,KilnContent.MACHINE_PARTS.get(),2,200,part("steel","plate",2),part("copper","rod",4));
        // Existing inexpensive storage stays available before the first iron; metal fittings are used on doors and vehicles.
        add(jobs,Items.CHEST,1,100,planks(8));add(jobs,Items.BARREL,1,100,planks(7));
        for(String wood:List.of("oak","spruce","birch","jungle","acacia","dark_oak","mangrove","cherry","bamboo","crimson","warped")){
            var timber=new Need(Ingredient.of(item("minecraft:"+wood+"_planks")),6);
            add(jobs,item("minecraft:"+wood+"_door"),3,100,timber,need(ManufacturingContent.FASTENERS.get(),3));
            add(jobs,item("minecraft:"+wood+"_trapdoor"),2,100,timber,need(ManufacturingContent.FASTENERS.get(),2));
            add(jobs,item("minecraft:"+wood+"_fence_gate"),1,100,new Need(timber.ingredient(),2),need(Items.STICK,4),need(ManufacturingContent.FASTENERS.get(),2));
        }
        add(jobs,WheelbarrowContent.ITEM.get(),1,200,planks(3),need(ManufacturingContent.WHEEL.get(),1),part("iron","rod",1),need(ManufacturingContent.FASTENERS.get(),4));
        add(jobs,Items.MINECART,1,200,part("iron","plate",4),part("iron","rod",2),need(KilnContent.MACHINE_PARTS.get(),1));
        add(jobs,CoalMiningContent.CART_ITEM.get(),1,200,planks(4),part("iron","rod",2),need(KilnContent.MACHINE_PARTS.get(),1),need(ManufacturingContent.FASTENERS.get(),4));
        add(jobs,Items.IRON_BARS,16,100,part("iron","rod",12));
        add(jobs,Items.IRON_DOOR,3,100,part("iron","plate",6),need(ManufacturingContent.FASTENERS.get(),3));
        add(jobs,Items.IRON_TRAPDOOR,1,100,part("iron","plate",4),need(ManufacturingContent.FASTENERS.get(),2));
        add(jobs,Items.RAIL,16,100,part("iron","rod",12),need(Items.STICK,1));
        add(jobs,Items.POWERED_RAIL,6,100,part("gold","rod",12),need(Items.STICK,1),need(Items.REDSTONE,1));
        AssemblyRecipes.add(jobs);
        assembly=List.copyOf(jobs);return assembly;
    }
    public static Need replaceIngots(Need n){
        if(n.count()==0)return n;
        for(String metal:List.of("iron","copper","steel","gold")){
            Item ingot=metal.equals("steel")?KilnContent.STEEL.get():item("minecraft:"+metal+"_ingot");
            if(n.ingredient().getItems().length==1&&n.ingredient().test(new ItemStack(ingot)))return part(metal,"plate",n.count());
        }
        return n;
    }
}
