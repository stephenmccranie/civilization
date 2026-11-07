package dev.civilization;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

/** Assembly bills replace the former crafting-table recipes; counts include only the controller/part itself. */
final class AssemblyRecipes {
    private static Item item(String id){return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));}
    private static WorkshopJobs.Need item(String id,int n){return new WorkshopJobs.Need(Ingredient.of(item(id)),n);}
    private static WorkshopJobs.Need tag(String id,int n){return new WorkshopJobs.Need(Ingredient.of(TagKey.create(Registries.ITEM,ResourceLocation.parse(id))),n);}
    private static void add(List<WorkshopJobs.Job> jobs,String id,int count,WorkshopJobs.Need... needs){
        var list=new ArrayList<>(List.of(needs));while(list.size()<4)list.add(WorkshopJobs.empty());
        if(list.size()!=4)throw new IllegalArgumentException(id);
        jobs.add(new WorkshopJobs.Job(item(id).getDescriptionId(),new ItemStack(item(id),count),List.copyOf(list),200));
    }
    static void add(List<WorkshopJobs.Job> jobs){
        add(jobs,"civilization:backpack",1,item("minecraft:leather",8),item("minecraft:string",4),item("minecraft:stick",4));
        add(jobs,"civilization:boat_helm",1,item("civilization:steel_plate",4),item("civilization:machine_parts",3),tag("minecraft:planks",1));
        add(jobs,"civilization:coal_preparation_screen",1,tag("minecraft:planks",4),item("civilization:iron_plate",2),item("minecraft:stick",2));
        add(jobs,"civilization:condenser",1,item("civilization:iron_plate",4),item("civilization:copper_plate",2),item("minecraft:iron_bars",2),item("civilization:machine_parts",1));
        add(jobs,"civilization:cooling_grille",3,item("civilization:iron_fasteners",6),item("civilization:industrial_casing",3));
        add(jobs,"civilization:distillation_column",1,item("civilization:iron_plate",4),item("civilization:copper_plate",4),item("civilization:machine_parts",1));
        add(jobs,"civilization:empty_canister",1,item("civilization:iron_plate",5));
        add(jobs,"civilization:fluid_pipe",8,item("civilization:iron_fasteners",4),item("civilization:copper_plate",2));
        add(jobs,"civilization:fuel_tank",1,item("civilization:iron_plate",4),item("civilization:copper_plate",4));
        add(jobs,"civilization:geiger_counter",1,item("minecraft:glass_pane",1),item("civilization:copper_plate",2),item("minecraft:redstone",1),item("civilization:machine_parts",1));
        add(jobs,"civilization:industrial_casing",8,item("civilization:steel_plate",4),item("civilization:copper_plate",4));
        add(jobs,"civilization:industrial_guardrail",16,item("civilization:copper_rod",6),item("civilization:iron_rod",10));
        add(jobs,"civilization:oil_pump",1,item("civilization:iron_plate",4),item("civilization:copper_plate",3),item("civilization:machine_parts",2));
        add(jobs,"civilization:oil_refinery",1,item("civilization:copper_plate",3),item("minecraft:brick",3),item("civilization:machine_parts",1),item("civilization:iron_plate",2));
        add(jobs,"civilization:prospecting_rod",1,item("civilization:iron_rod",1),item("minecraft:stick",2));
        add(jobs,"civilization:refinery_flue",4,item("civilization:industrial_casing",4));
        add(jobs,"civilization:refinery_port",2,item("civilization:iron_plate",4),item("civilization:copper_plate",1));
        add(jobs,"civilization:land_controller",1,item("minecraft:stone_bricks",4),item("civilization:mineral_coal",4),item("minecraft:iron_block",1));
        add(jobs,"civilization:survey_table_section",1,item("minecraft:paper",1),item("minecraft:oak_slab",1),item("civilization:copper_plate",1));
        add(jobs,"civilization:trade_counter",1,tag("minecraft:planks",6),item("civilization:iron_plate",2),item("minecraft:chest",1));
        add(jobs,"civilization:gladiator_pit",1,item("minecraft:stone_bricks",4),item("civilization:copper_plate",4),item("civilization:iron_plate",1));
        add(jobs,"civilization:coal_drill",1,item("civilization:iron_plate",5),item("civilization:copper_plate",2),item("minecraft:iron_pickaxe",1),item("civilization:machine_parts",1));
    }
}
