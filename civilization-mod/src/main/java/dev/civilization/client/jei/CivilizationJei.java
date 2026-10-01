package dev.civilization.client.jei;

import dev.civilization.*;
import java.util.*;
import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** Optional, read-only recipe views. JEI discovers this class; common/server code never references it. */
@JeiPlugin
public final class CivilizationJei implements IModPlugin {
    public record Slot(RecipeIngredientRole role,List<ItemStack> items,FluidStack fluid,String hint) {}
    public record Process(ResourceLocation id,List<Slot> slots,int ticks,String note) {}
    private final Map<String,Category> categories=new LinkedHashMap<>();
    private static IJeiRuntime runtime;
    public static IJeiRuntime runtime(){return runtime;}
    public static mezz.jei.api.recipe.RecipeType<Process> type(String name){return mezz.jei.api.recipe.RecipeType.create("civilization",name,Process.class);}
    @Override public ResourceLocation getPluginUid(){return ResourceLocation.parse("civilization:recipes");}
    @Override public void onRuntimeAvailable(IJeiRuntime value){
        runtime=value;
        // JEI synthesizes these vanilla pages independently of the recipe manager.
        var manager=value.getRecipeManager();
        manager.hideRecipeCategory(mezz.jei.api.constants.RecipeTypes.FUELING);
        manager.hideRecipes(mezz.jei.api.constants.RecipeTypes.ANVIL,manager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.ANVIL).get()
                .filter(r->r.getLeftInputs().stream().anyMatch(WorkshopJobs::gated)&&r.getRightInputs().stream().noneMatch(s->s.is(Items.ENCHANTED_BOOK))).toList());
        manager.hideRecipes(mezz.jei.api.constants.RecipeTypes.GRINDSTONE,manager.createRecipeLookup(mezz.jei.api.constants.RecipeTypes.GRINDSTONE).get()
                .filter(r->r.getTopInputs().stream().anyMatch(WorkshopJobs::gated)&&r.getBottomInputs().stream().anyMatch(s->!s.isEmpty())).toList());
        value.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,List.of(FarmingContent.MINERAL_BLEND.toStack()));
        value.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,VanillaRetirement.ITEMS.stream().map(ItemStack::new).toList());
    }
    @Override public void onRuntimeUnavailable(){runtime=null;}
    @Override public void registerItemSubtypes(ISubtypeRegistration r){
        r.registerSubtypeInterpreter(CuttingContent.ITEM.get(),new ISubtypeInterpreter<ItemStack>(){
            public Object getSubtypeData(ItemStack stack,UidContext context){return getLegacyStringSubtypeInfo(stack,context);}
            public String getLegacyStringSubtypeInfo(ItemStack stack,UidContext context){return BuiltInRegistries.BLOCK.getKey(CuttingContent.material(stack).getBlock())+"/"+CuttingContent.units(stack);}
        });
    }
    @Override public void registerGuiHandlers(IGuiHandlerRegistration r){
        r.addGuiContainerHandler(dev.civilization.client.KilnScreen.class,new mezz.jei.api.gui.handlers.IGuiContainerHandler<dev.civilization.client.KilnScreen>(){
            public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(dev.civilization.client.KilnScreen s){return List.of(s.recipeDrawerBounds());}
            public Collection<mezz.jei.api.gui.handlers.IGuiClickableArea> getGuiClickableAreas(dev.civilization.client.KilnScreen s,double x,double y){
                String name=s.getMenu().tooltipPrefix();if(name.equals("retort"))name="fertilizer";
                return List.of(mezz.jei.api.gui.handlers.IGuiClickableArea.createBasic(73,49,43,12,type(name)));
            }
        });
        r.addGuiContainerHandler(dev.civilization.client.WorkshopScreen.class,new mezz.jei.api.gui.handlers.IGuiContainerHandler<dev.civilization.client.WorkshopScreen>(){
            public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(dev.civilization.client.WorkshopScreen s){return List.of(s.recipeDrawerBounds());}
            public Collection<mezz.jei.api.gui.handlers.IGuiClickableArea> getGuiClickableAreas(dev.civilization.client.WorkshopScreen s,double x,double y){return List.of(mezz.jei.api.gui.handlers.IGuiClickableArea.createBasic(94,49,23,12,type(new String[]{"tannery","textile","smithy"}[s.getMenu().kind()])));}
        });
    }
    @Override public void registerCategories(IRecipeCategoryRegistration r){
        categories.clear();
        category(r,"kiln",KilnContent.KILN.get());category(r,"foundry",KilnContent.FOUNDRY.get());
        category(r,"fertilizer",KilnContent.RETORT.get());category(r,"cooking",CookingContent.STATION.get());
        category(r,"tannery",WorkshopContent.TANNERY.get());category(r,"textile",WorkshopContent.TEXTILE.get());category(r,"smithy",WorkshopContent.SMITHY.get());
        category(r,"oil_pump",IndustrialContent.PUMP.get());category(r,"heater",IndustrialContent.REFINERY.get());
        category(r,"column",IndustrialContent.COLUMN.get());category(r,"condenser",IndustrialContent.CONDENSER.get());category(r,"coal_drill",IndustrialContent.DRILL.get());
        category(r,"canisters",IndustrialContent.TANK.get(),"Canister filling / emptying");
        category(r,"cutting",CuttingContent.STONE_SAW.get(),"Saw cutting");category(r,"recombining",Items.CRAFTING_TABLE,"Recombining cut blocks");
    }
    private void category(IRecipeCategoryRegistration r,String name,ItemLike controller){category(r,name,controller,new ItemStack(controller).getHoverName().getString());}
    private void category(IRecipeCategoryRegistration r,String name,ItemLike controller,String title){
        var category=new Category(name,controller,title,r.getJeiHelpers().getGuiHelper().createDrawableItemLike(controller));
        categories.put(name,category);r.addRecipeCategories(category);
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){
        for(var c:categories.values())r.addRecipeCatalyst(c.controller,c.getRecipeType());
        r.addRecipeCatalyst(CuttingContent.IRON_SAW.get(),type("cutting"));r.addRecipeCatalyst(CuttingContent.DIAMOND_SAW.get(),type("cutting"));
    }
    private static Slot item(RecipeIngredientRole role,ItemStack stack){return new Slot(role,List.of(stack),FluidStack.EMPTY,"");}
    private static Slot input(ItemLike item,int count){return item(RecipeIngredientRole.INPUT,new ItemStack(item,count));}
    private static Slot output(ItemLike item,int count){return item(RecipeIngredientRole.OUTPUT,new ItemStack(item,count));}
    private static Slot fluid(RecipeIngredientRole role,Fluid fluid,int amount){return new Slot(role,List.of(),new FluidStack(fluid,amount),"");}
    private static Slot ingredient(Ingredient ingredient,int count){return new Slot(RecipeIngredientRole.INPUT,Arrays.stream(ingredient.getItems()).map(s->s.copyWithCount(count)).toList(),FluidStack.EMPTY,"");}
    private static Slot coal(){return new Slot(RecipeIngredientRole.CATALYST,List.of(KilnContent.MINERAL_COAL.toStack()),FluidStack.EMPTY,"Fuel supply; not one Coal per batch. Idle fires also burn Coal.");}
    private static Process process(String id,List<Slot> slots,int ticks,String note){return new Process(ResourceLocation.parse("civilization:"+id),List.copyOf(slots),ticks,note);}
    private void add(IRecipeRegistration r,String name,List<Process> recipes){r.addRecipes(type(name),recipes);}
    @Override public void registerRecipes(IRecipeRegistration r){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        cooking(r,"kiln",KilnContent.RECIPE_TYPE.get());cooking(r,"foundry",KilnContent.FOUNDRY_RECIPE_TYPE.get());
        cooking(r,"fertilizer",KilnContent.RETORT_RECIPE_TYPE.get());cooking(r,"cooking",CookingContent.RECIPE_TYPE.get());
        for(int kind=0;kind<3;kind++){
            var recipes=new ArrayList<Process>();
            for(var job:WorkshopJobs.jobs(kind)){
                if(job.output().isEmpty())continue;
                var slots=new ArrayList<Slot>();for(var need:job.needs())if(need.count()>0)slots.add(ingredient(need.ingredient(),need.count()));
                slots.add(item(RecipeIngredientRole.OUTPUT,job.output().copy()));slots.add(coal());
                boolean upgrade=BuiltInRegistries.ITEM.getKey(job.output().getItem()).getPath().startsWith("netherite_");
                recipes.add(process("workshop_"+kind+"/"+job.id().replace(':','/'),slots,job.ticks(),job.coalNote()+(upgrade?" Upgrade keeps enchantments and existing wear.":" Light the fire in the controller.")));
            }
            if(kind==2)for(var item:BuiltInRegistries.ITEM){
                var damaged=new ItemStack(item);if(!WorkshopJobs.gated(damaged))continue;
                damaged.setDamageValue(Math.max(1,damaged.getMaxDamage()/2));var job=WorkshopJobs.repair(damaged);if(job==null)continue;
                var slots=new ArrayList<Slot>();slots.add(item(RecipeIngredientRole.INPUT,damaged));
                for(int i=1;i<job.needs().size();i++){var need=job.needs().get(i);if(need.count()>0)slots.add(ingredient(need.ingredient(),need.count()));}
                slots.add(item(RecipeIngredientRole.OUTPUT,job.output()));slots.add(coal());
                recipes.add(process("repair/"+BuiltInRegistries.ITEM.getKey(item).getPath(),slots,job.ticks(),job.coalNote()+" Repair example: 50% damaged. Materials scale with damage. 10% wear, 25% max-durability floor; no XP; enchants stay."));
            }
            add(r,new String[]{"tannery","textile","smithy"}[kind],recipes);
        }
        industry(r);cutting(r);
        r.addItemStackInfo(KilnContent.FOUNDRY.toStack(),Component.literal("First metals: fire clay into bricks in the Kiln, then build the Foundry. Its controller is eight brick blocks around cobblestone; its structure needs only brick pieces and a stone saw. Smelt iron/copper/gold here. The Smithy makes equipment from the ingots."));
        r.addItemStackInfo(KilnContent.MINERAL_COAL.toStack(),Component.literal("Coal machines require manual ignition and keep burning while idle. Consumption depends on work, idle time and temperature; JEI shows baseline processing times, not a fixed Coal charge per recipe."));
    }
    private <T extends AbstractCookingRecipe> void cooking(IRecipeRegistration r,String name,net.minecraft.world.item.crafting.RecipeType<T> recipeType){
        var level=Minecraft.getInstance().level;
        var recipes=level.getRecipeManager().getAllRecipesFor(recipeType).stream().sorted(Comparator.comparing(h->h.id().toString())).map(h->{
            var recipe=h.value();return new Process(h.id(),List.of(ingredient(recipe.getIngredients().getFirst(),1),item(RecipeIngredientRole.OUTPUT,recipe.getResultItem(level.registryAccess()).copy()),coal()),recipe.getCookingTime(),"One Coal supplies 20s of base heat. Select the recipe in the controller; light its fire.");
        }).toList();add(r,name,recipes);
    }
    private void industry(IRecipeRegistration r){
        for(var kind:List.of(IndustrialBlock.Kind.REFINERY,IndustrialBlock.Kind.COLUMN,IndustrialBlock.Kind.CONDENSER)){
            var batch=IndustrialProcess.forKind(kind);var slots=new ArrayList<Slot>();
            slots.add(fluid(RecipeIngredientRole.INPUT,batch.input(),batch.inputMb()));slots.add(fluid(RecipeIngredientRole.OUTPUT,batch.output(),batch.outputMb()));
            if(batch.oilMb()>0)slots.add(fluid(RecipeIngredientRole.OUTPUT,IndustrialContent.LUBE.get(),batch.oilMb()));
            if(batch.sulfur()>0)slots.add(output(IndustrialContent.SULFUR.get(),batch.sulfur()));
            boolean fired=kind==IndustrialBlock.Kind.REFINERY;if(fired)slots.add(coal());
            String name=fired?"heater":kind==IndustrialBlock.Kind.COLUMN?"column":"condenser";
            add(r,name,List.of(process(name,slots,batch.ticks(),fired?"Coal heats 250 mB at baseline. Temperature and idle burning affect consumption.":"Connect the multiblock fluid ports with pipes. All output buffers need room.")));
        }
        add(r,"oil_pump",List.of(process("oil_extraction",List.of(fluid(RecipeIngredientRole.OUTPUT,IndustrialContent.CRUDE.get(),125),coal()),50,"Build above the physical oil deposit within its footprint. Drains 125 mB per step. One Coal extracts 250 mB before idle losses.")));
        add(r,"coal_drill",List.of(process("coal_extraction",List.of(fluid(RecipeIngredientRole.INPUT,IndustrialContent.FUEL.get(),IndustrialRates.DRILL_FUEL_MB),
                new Slot(RecipeIngredientRole.CATALYST,List.of(),new FluidStack(IndustrialContent.LUBE.get(),10),"Optional: consumed per batch to restore full speed; dry drilling slows to 20%."),output(KilnContent.MINERAL_COAL.get(),16)),100,"Breaks one physical coal-seam block below the drill. 5s lubricated; up to 25s dry. Oil is optional, fuel is required.")));
        var cans=new ArrayList<Process>();
        for(var fluid:List.of(IndustrialContent.CRUDE.get(),IndustrialContent.FUEL.get(),IndustrialContent.LUBE.get())){
            var full=IndustrialContent.can(new FluidStack(fluid,1000));String id=BuiltInRegistries.FLUID.getKey(fluid).getPath();
            cans.add(process("fill_"+id,List.of(input(IndustrialContent.CAN.get(),1),fluid(RecipeIngredientRole.INPUT,fluid,1000),output(full,1)),0,"Right-click a compatible controller with a canister. Each canister holds 1,000 mB."));
            cans.add(process("empty_"+id,List.of(input(full,1),output(IndustrialContent.CAN.get(),1),fluid(RecipeIngredientRole.OUTPUT,fluid,1000)),0,"Empty into a compatible controller with room for the full 1,000 mB. The empty canister is returned."));
        }add(r,"canisters",cans);
    }
    private void cutting(IRecipeRegistration r){
        var level=Minecraft.getInstance().level;var cut=new CuttingRecipe(CraftingBookCategory.MISC);var join=new RecombiningRecipe(CraftingBookCategory.MISC);
        var cuts=new ArrayList<Process>();var joins=new ArrayList<Process>();
        for(var block:BuiltInRegistries.BLOCK){
            if(block.asItem()==Items.AIR||!CuttingContent.cuttable(block.defaultBlockState())||VanillaRetirement.retired(new ItemStack(block)))continue;
            String id=BuiltInRegistries.BLOCK.getKey(block).toString().replace(':','/');
            ItemStack source=new ItemStack(block);
            for(int step=0;step<3;step++){
                var saws=new ArrayList<ItemStack>();ItemStack result=ItemStack.EMPTY;
                for(var saw:List.of(CuttingContent.STONE_SAW.get(),CuttingContent.IRON_SAW.get(),CuttingContent.DIAMOND_SAW.get())){
                    var grid=CraftingInput.of(2,1,List.of(source,new ItemStack(saw)));
                    if(cut.matches(grid,level)){saws.add(new ItemStack(saw));result=cut.assemble(grid,level.registryAccess());}
                }
                if(result.isEmpty())break;
                cuts.add(process("cut/"+id+"/"+step,List.of(item(RecipeIngredientRole.INPUT,source),new Slot(RecipeIngredientRole.CATALYST,saws,FluidStack.EMPTY,"Place the saw beside the block in a crafting grid. Costs one durability."),item(RecipeIngredientRole.OUTPUT,result)),0,"Crafting grid: block + saw. Two pieces; one saw durability. Only suitable saw tiers are shown."));
                var piece=result.copyWithCount(1);var grid=CraftingInput.of(2,1,List.of(piece,piece.copy()));
                if(join.matches(grid,level))joins.add(process("join/"+id+"/"+step,List.of(item(RecipeIngredientRole.INPUT,piece),item(RecipeIngredientRole.INPUT,piece.copy()),item(RecipeIngredientRole.OUTPUT,join.assemble(grid,level.registryAccess()))),0,"Place one matching piece in each of two crafting slots. No saw required."));
                source=piece;
            }
        }add(r,"cutting",cuts);add(r,"recombining",joins);
    }
    private static final class Category implements IRecipeCategory<Process>{
        private final String name;private final ItemLike controller;private final Component title;private final IDrawable icon;
        Category(String name,ItemLike controller,String title,IDrawable icon){this.name=name;this.controller=controller;this.title=Component.literal(title);this.icon=icon;}
        public mezz.jei.api.recipe.RecipeType<Process> getRecipeType(){return type(name);}
        public Component getTitle(){return title;}public IDrawable getIcon(){return icon;}
        public int getWidth(){return 190;}public int getHeight(){return 138;}
        @Override public ResourceLocation getRegistryName(Process recipe){return recipe.id();}
        public void setRecipe(IRecipeLayoutBuilder builder,Process recipe,IFocusGroup focuses){
            int inputs=0,outputs=0,catalysts=0;
            for(var slot:recipe.slots()){
                int x,y;
                if(slot.role()==RecipeIngredientRole.OUTPUT){x=122+22*outputs++;y=20;}
                else if(slot.role()==RecipeIngredientRole.CATALYST){x=12+22*catalysts++;y=48;}
                else {x=12+22*inputs++;y=20;}
                var b=builder.addSlot(slot.role(),x,y).setStandardSlotBackground();
                if(slot.fluid().isEmpty())b.addItemStacks(slot.items());
                else b.addFluidStack(slot.fluid().getFluid(),slot.fluid().getAmount()).setCustomRenderer(mezz.jei.api.neoforge.NeoForgeTypes.FLUID_STACK,FLUID_RENDERER);
                if(!slot.hint().isEmpty())b.addRichTooltipCallback((view,tooltip)->tooltip.add(Component.literal(slot.hint())));
            }
        }
        @Override public void draw(Process recipe,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){
            var font=Minecraft.getInstance().font;
            g.fill(0,0,190,138,0xffc4baa5);g.fill(0,0,190,2,0xff75634b);g.fill(0,136,190,138,0xff75634b);
            g.drawString(font,"Ingredients",10,6,0x38352e,false);g.drawString(font,"Products",122,6,0x38352e,false);
            g.drawString(font,"→",102,24,0x57472e,false);
            if(recipe.ticks()>0)g.drawString(font,String.format(Locale.ROOT,"%.1fs",recipe.ticks()/20d),126,48,0x38352e,false);
            recipe.slots().stream().filter(s->s.role()==RecipeIngredientRole.CATALYST).findFirst().ifPresent(s->{
                String label=!s.fluid().isEmpty()?"Optional oil":s.items().stream().anyMatch(i->i.is(KilnContent.MINERAL_COAL.get()))?"Fuel":"Saw";
                g.drawString(font,label,34,52,0x494438,false);
            });
            int y=76;for(var line:font.split(Component.literal(recipe.note()),172)){g.drawString(font,line,9,y,0x494438,false);y+=10;}
        }
    }
    private static final mezz.jei.api.ingredients.IIngredientRenderer<FluidStack> FLUID_RENDERER=new mezz.jei.api.ingredients.IIngredientRenderer<>(){
        public void render(GuiGraphics g,FluidStack stack){
            g.fill(0,0,16,16,dev.civilization.client.IndustrialScreen.color(IndustrialContent.fluidId(stack)));
            g.fill(1,1,3,14,0x44ffffff);
            var font=Minecraft.getInstance().font;String amount=stack.getAmount()==1000?"1k":Integer.toString(stack.getAmount());
            g.pose().pushPose();g.pose().translate(16,11,200);g.pose().scale(.65f,.65f,1);
            g.drawString(font,amount,-font.width(amount),0,0xffffffff,true);g.pose().popPose();
        }
        public List<Component> getTooltip(FluidStack stack,TooltipFlag flag){return List.of(stack.getHoverName(),Component.literal(String.format(Locale.ROOT,"%,d mB",stack.getAmount())));}
    };
}
