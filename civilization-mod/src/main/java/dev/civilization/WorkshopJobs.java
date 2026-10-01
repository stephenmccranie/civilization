package dev.civilization;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;

/** A bounded equipment catalog, shared by quotations, crafting gates and the menu. */
public final class WorkshopJobs {
    public record Need(Ingredient ingredient,int count){
        public boolean matches(ItemStack s){return count==0?s.isEmpty():s.getCount()>=count&&ingredient.test(s);}
        public ItemStack icon(){return count==0?ItemStack.EMPTY:ingredient.getItems()[0].copyWithCount(count);}
    }
    public record Job(String name,ItemStack output,List<Need> needs,int ticks,int workTicksPerHeat){
        public Job {if(workTicksPerHeat<1)throw new IllegalArgumentException("Work ticks per heat must be positive");}
        public Job(String name,ItemStack output,List<Need> needs,int ticks){this(name,output,needs,ticks,1);}
        /** The output registry ID is stable when catalog rows are reordered. Repair has no output template. */
        public String id(){return output.isEmpty()?"civilization:repair":BuiltInRegistries.ITEM.getKey(output.getItem()).toString();}
        /** Catalog durations are base ticks; all consumers use the material-scaled accessor. */
        public int ticks(){return ticks*materialMultiplier();}
        public int heatSpent(int completedTicks){return completedTicks/workTicksPerHeat;}
        public int materialMultiplier(){
            var e=equipment(output);if(e==null)return 1;
            return e.material()==Items.NETHERITE_INGOT?8:e.material()==Items.DIAMOND?4:e.material()==Items.GOLD_INGOT?2:1;
        }
        public String coalNote(){return String.format(Locale.ROOT,"Base coal: %.2f per batch%s. Temperature and idle burn affect usage.",heatSpent(ticks())/(double)ProductionEnergy.HEAT_TICKS,workTicksPerHeat==1?String.format(Locale.ROOT," (%dx duration)",materialMultiplier()):" (gentle heat)");}
    }
    public record Equipment(Item item,Item material,int amount,boolean armor){}
    private static List<Job> smithy;
    private static final Map<Item,Equipment> EQUIPMENT=new LinkedHashMap<>();
    public static Need need(Item item,int count){return count==0?empty():new Need(Ingredient.of(item),count);}
    public static Need empty(){return new Need(Ingredient.EMPTY,0);}
    private static Item item(String name){return BuiltInRegistries.ITEM.get(ResourceLocation.parse(name));}
    public static synchronized List<Job> smithy(){
        if(smithy!=null)return smithy;
        var a=new ArrayList<Job>();a.add(new Job("Repair",ItemStack.EMPTY,List.of(),400));
        String[] shapes={"pickaxe","axe","shovel","hoe","sword","helmet","chestplate","leggings","boots"};
        int[] metals={3,3,1,2,2,5,8,7,4}, sticks={2,2,2,2,1,0,0,0,0};
        for(String tier:List.of("iron","golden","diamond"))for(int i=0;i<shapes.length;i++){
            Item result=item("minecraft:"+tier+"_"+shapes[i]);Item material=tier.equals("diamond")?Items.DIAMOND:tier.equals("golden")?Items.GOLD_INGOT:Items.IRON_INGOT;
            add(a,result,material,metals[i],i>=5,sticks[i]);
        }
        add(a,CuttingContent.IRON_SAW.get(),Items.IRON_INGOT,3,false,2);
        add(a,CuttingContent.DIAMOND_SAW.get(),Items.DIAMOND,3,false,2);
        add(a,Items.SHEARS,Items.IRON_INGOT,2,false,0);
        for(int i=5;i<shapes.length;i++)add(a,item("minecraft:chainmail_"+shapes[i]),Items.IRON_INGOT,metals[i],true,0);
        for(int i=0;i<shapes.length;i++){
            Item result=item("minecraft:netherite_"+shapes[i]),base=item("minecraft:diamond_"+shapes[i]);
            EQUIPMENT.put(result,new Equipment(result,Items.NETHERITE_INGOT,1,i>=5));
            a.add(new Job(result.getDescriptionId(),new ItemStack(result),List.of(need(base,1),need(Items.NETHERITE_INGOT,1),need(i>=5?WorkshopContent.CLOTH.get():Items.LEATHER,i>=5?2:1),need(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,1)),i>=5?1200:600));
        }
        a.add(new Job("Coal Bunker Controller",BulkContent.BUNKER.toStack(),List.of(need(KilnContent.STEEL.get(),4),need(KilnContent.MACHINE_PARTS.get(),1),empty(),empty()),1200));
        a.add(new Job("Cargo Tank Controller",BulkContent.TANK.toStack(),List.of(need(KilnContent.STEEL.get(),4),need(KilnContent.MACHINE_PARTS.get(),1),need(Items.GLASS,2),empty()),1200));
        a.add(new Job("Hot-Bulb Engine",new ItemStack(OilEngineContent.ENGINE.get()),List.of(need(KilnContent.STEEL.get(),4),need(KilnContent.MACHINE_PARTS.get(),2),need(Items.COPPER_INGOT,2),need(Items.IRON_INGOT,4)),1200));
        EQUIPMENT.put(Items.SHIELD,new Equipment(Items.SHIELD,Items.IRON_INGOT,1,false));
        a.add(new Job(Items.SHIELD.getDescriptionId(),new ItemStack(Items.SHIELD),List.of(
                need(Items.IRON_INGOT,1),new Need(Ingredient.of(ItemTags.PLANKS),6),need(Items.LEATHER,1),empty()),600));
        smithy=List.copyOf(a);return smithy;
    }
    private static void add(List<Job> jobs,Item result,Item material,int amount,boolean armor,int sticks){
        EQUIPMENT.put(result,new Equipment(result,material,amount,armor));
        jobs.add(new Job(result.getDescriptionId(),new ItemStack(result),List.of(need(material,amount),need(Items.LEATHER,1),armor?need(WorkshopContent.CLOTH.get(),2):empty(),need(Items.STICK,sticks)),result==Items.SHEARS?400:armor?1200:600));
    }
    public static Equipment equipment(ItemStack s){smithy();return EQUIPMENT.get(s.getItem());}
    public static boolean gated(ItemStack s){return equipment(s)!=null;}
    public static int index(int kind,String id){var jobs=jobs(kind);for(int i=0;i<jobs.size();i++)if(jobs.get(i).id().equals(id))return i;return -1;}
    public static List<Job> jobs(int kind){return switch(kind){
        case 0->List.of(new Job("Tanning",new ItemStack(Items.LEATHER),List.of(need(WorkshopContent.HIDE.get(),1),empty(),empty(),empty()),200,2));
        case 1->List.of(new Job("Weaving",WorkshopContent.CLOTH.toStack(2),List.of(new Need(Ingredient.of(ItemTags.WOOL),1),empty(),empty(),empty()),200,2));
        default->smithy();};}
    public static int originalMax(ItemStack stack){
        int base=stack.getItem().components().getOrDefault(DataComponents.MAX_DAMAGE,stack.getMaxDamage());
        return EquipmentGrade.hasGrade(stack)?Math.max(1,(int)Math.round(base*EquipmentGrade.value(stack)/100.0)):base;
    }
    public static Job repair(ItemStack stack){
        var e=equipment(stack);if(e==null||!stack.isDamaged()||stack.getCount()!=1)return null;
        int repairedGrade=EquipmentGrade.afterRepair(stack);
        if(repairedGrade<=0)return null;
        int count=Math.max(1,(int)Math.ceil(e.amount()*.5*stack.getDamageValue()/stack.getMaxDamage()));
        var result=stack.copy();EquipmentGrade.apply(result,repairedGrade);result.setDamageValue(0);
        result.remove(DataComponents.REPAIR_COST);
        return new Job("Repair",result,List.of(need(stack.getItem(),1),need(e.material(),count),need(e.armor()?WorkshopContent.CLOTH.get():Items.LEATHER,1),empty()),400);
    }
    public static ItemStack result(Job job,ItemStack first){
        var out=job.output().copy();
        if(first.getItem() instanceof TieredItem||first.getItem() instanceof ArmorItem){
            if(!job.name().equals("Repair")&&BuiltInRegistries.ITEM.getKey(out.getItem()).getPath().startsWith("netherite_")){
                out.applyComponents(first.getComponentsPatch());
                EquipmentGrade.apply(out,EquipmentGrade.value(first));
                out.setDamageValue((int)Math.ceil(first.getDamageValue()/(double)first.getMaxDamage()*out.getMaxDamage()));
            }
        }
        return out;
    }
}
