package dev.civilization;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.fluids.*;

public final class IndustrialContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks("civilization");
    private static final DeferredRegister.Items ITEMS=DeferredRegister.createItems("civilization");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,"civilization");
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"civilization");
    private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES,"civilization");
    private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"civilization");
    private static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,"civilization");
    public static final TagKey<Biome> COAL_REGIONS=TagKey.create(Registries.BIOME,ResourceLocation.parse("civilization:coal_regions")), OIL_REGIONS=TagKey.create(Registries.BIOME,ResourceLocation.parse("civilization:oil_regions"));
    public static final DeferredHolder<FluidType,FluidType> CRUDE_TYPE=TYPES.register("crude_oil",()->new FluidType(FluidType.Properties.create().density(900).viscosity(3000).canConvertToSource(false)){});
    public static final DeferredHolder<FluidType,FluidType> FUEL_TYPE=TYPES.register("refined_fuel",()->new FluidType(FluidType.Properties.create().density(800).viscosity(1500).canConvertToSource(false)){});
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> CRUDE=FLUIDS.register("crude_oil",()->new ReservoirOil.Source(crude()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> CRUDE_FLOWING=FLUIDS.register("flowing_crude_oil",()->new ReservoirOil.Flowing(crude()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> FUEL=FLUIDS.register("refined_fuel",()->new BaseFlowingFluid.Source(fuel()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> FUEL_FLOWING=FLUIDS.register("flowing_refined_fuel",()->new BaseFlowingFluid.Flowing(fuel()));
    private static BaseFlowingFluid.Properties crude(){return new BaseFlowingFluid.Properties(CRUDE_TYPE,CRUDE,CRUDE_FLOWING).block(SURFACE_OIL).tickRate(16);}
    private static BaseFlowingFluid.Properties fuel(){return new BaseFlowingFluid.Properties(FUEL_TYPE,FUEL,FUEL_FLOWING);}
    public static final DeferredHolder<FluidType,FluidType> HEATED_TYPE=TYPES.register("heated_crude",()->new FluidType(FluidType.Properties.create().density(850).canConvertToSource(false)){});
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> HEATED=FLUIDS.register("heated_crude",()->new BaseFlowingFluid.Source(heated()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> HEATED_FLOWING=FLUIDS.register("flowing_heated_crude",()->new BaseFlowingFluid.Flowing(heated()));
    private static BaseFlowingFluid.Properties heated(){return new BaseFlowingFluid.Properties(HEATED_TYPE,HEATED,HEATED_FLOWING);}
    public static final DeferredHolder<FluidType,FluidType> VAPOR_TYPE=TYPES.register("distillate_vapor",()->new FluidType(FluidType.Properties.create().density(50).canConvertToSource(false)){});
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> VAPOR=FLUIDS.register("distillate_vapor",()->new BaseFlowingFluid.Source(vapor()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> VAPOR_FLOWING=FLUIDS.register("flowing_distillate_vapor",()->new BaseFlowingFluid.Flowing(vapor()));
    private static BaseFlowingFluid.Properties vapor(){return new BaseFlowingFluid.Properties(VAPOR_TYPE,VAPOR,VAPOR_FLOWING);}
    public static final DeferredHolder<FluidType,FluidType> LUBE_TYPE=TYPES.register("lubricating_oil",()->new FluidType(FluidType.Properties.create().density(900).canConvertToSource(false)){});
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Source> LUBE=FLUIDS.register("lubricating_oil",()->new BaseFlowingFluid.Source(lube()));
    public static final DeferredHolder<Fluid,BaseFlowingFluid.Flowing> LUBE_FLOWING=FLUIDS.register("flowing_lubricating_oil",()->new BaseFlowingFluid.Flowing(lube()));
    private static BaseFlowingFluid.Properties lube(){return new BaseFlowingFluid.Properties(LUBE_TYPE,LUBE,LUBE_FLOWING);}
    public static final DeferredBlock<IndustrialBlock> PUMP=machine("oil_pump",IndustrialBlock.Kind.PUMP), REFINERY=machine("oil_refinery",IndustrialBlock.Kind.REFINERY), DRILL=machine("coal_drill",IndustrialBlock.Kind.DRILL), TANK=machine("fuel_tank",IndustrialBlock.Kind.TANK), COLUMN=machine("distillation_column",IndustrialBlock.Kind.COLUMN), CONDENSER=machine("condenser",IndustrialBlock.Kind.CONDENSER);
    private static DeferredBlock<IndustrialBlock> machine(String name,IndustrialBlock.Kind kind){var b=BLOCKS.register(name,()->new IndustrialBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).pushReaction(PushReaction.BLOCK),kind));ITEMS.registerSimpleBlockItem(b);return b;}
    public static final DeferredBlock<DerrickPartBlock> DERRICK_PART=BLOCKS.register("derrick_part",()->new DerrickPartBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<IndustrialGuardrailBlock> GUARDRAIL=BLOCKS.register("industrial_guardrail",()->new IndustrialGuardrailBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<Block> CASING=BLOCKS.register("industrial_casing",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredBlock<Block> COOLING=BLOCKS.register("cooling_grille",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    public static final DeferredBlock<RefineryFlueBlock> FLUE=BLOCKS.register("refinery_flue",()->new RefineryFlueBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).noOcclusion()));
    public static final DeferredBlock<FluidPipeBlock> PIPE=BLOCKS.register("fluid_pipe",()->new FluidPipeBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion()));
    public static final DeferredBlock<RefineryPortBlock> PORT=BLOCKS.register("refinery_port",()->new RefineryPortBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)));
    static {ITEMS.registerSimpleBlockItem(CASING);ITEMS.registerSimpleBlockItem(GUARDRAIL);ITEMS.registerSimpleBlockItem(COOLING);ITEMS.registerSimpleBlockItem(FLUE);ITEMS.registerSimpleBlockItem(PIPE);ITEMS.registerSimpleBlockItem(PORT);}
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<PipeFlowEntity>> PIPE_ENTITY=ENTITIES.register("pipe_flow",()->BlockEntityType.Builder.of(PipeFlowEntity::new,PIPE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<RefineryPortEntity>> PORT_ENTITY=ENTITIES.register("refinery_port",()->BlockEntityType.Builder.of(RefineryPortEntity::new,PORT.get()).build(null));
    public static final DeferredItem<Item> LUBE_CAN=ITEMS.registerSimpleItem("lubricant_canister",new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SULFUR=ITEMS.registerSimpleItem("sulfur");
    public static final DeferredItem<Item> ENRICHED_BLEND=ITEMS.registerSimpleItem("enriched_mineral_blend");
    public static final DeferredBlock<Block> COAL_SEAM=BLOCKS.register("coal_seam",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_COAL_ORE).strength(-1,3600000).noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<Block> OIL_SEEP=BLOCKS.register("oil_seep",()->new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).strength(-1,3600000).noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<ReservoirOil> RESERVOIR_OIL=BLOCKS.register("reservoir_oil",()->new ReservoirOil(CRUDE.get(),BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(-1,3600000).noLootTable().pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<LiquidBlock> SURFACE_OIL=BLOCKS.register("surface_oil",()->new LiquidBlock(CRUDE.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).noLootTable()));
    public static final DeferredItem<ProspectingRod> PROBE=ITEMS.register("prospecting_rod",()->new ProspectingRod(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CAN=ITEMS.registerSimpleItem("empty_canister",new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> CRUDE_CAN=ITEMS.registerSimpleItem("crude_canister",new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> FUEL_CAN=ITEMS.registerSimpleItem("fuel_canister",new Item.Properties().stacksTo(1));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<IndustrialBlockEntity>> ENTITY=ENTITIES.register("industry",()->BlockEntityType.Builder.of(IndustrialBlockEntity::new,PUMP.get(),REFINERY.get(),DRILL.get(),TANK.get(),COLUMN.get(),CONDENSER.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<IndustrialMenu>> MENU=MENUS.register("industry",()->new MenuType<>(IndustrialMenu::new,net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    static {FEATURES.register("deposit",DepositFeature::new);}
    public static boolean industrial(FluidStack stack){return stack.is(CRUDE.get())||stack.is(FUEL.get())||stack.is(LUBE.get())||stack.is(HEATED.get())||stack.is(VAPOR.get());}
    public static int fluidId(FluidStack s){return s.is(CRUDE.get())?1:s.is(FUEL.get())?2:s.is(HEATED.get())?3:s.is(VAPOR.get())?4:s.is(LUBE.get())?5:0;}
    public static Item can(FluidStack s){return s.is(CRUDE.get())?CRUDE_CAN.get():s.is(FUEL.get())?FUEL_CAN.get():s.is(LUBE.get())?LUBE_CAN.get():null;}
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);TYPES.register(bus);FLUIDS.register(bus);FEATURES.register(bus);bus.addListener(IndustrialContent::capabilities);}
    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e){e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,PORT_ENTITY.get(),(port,side)->side==null?null:port.handler(side));e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,ENTITY.get(),(machine,side)->side==null?null:new IndustrialPort(machine,side));}
}
