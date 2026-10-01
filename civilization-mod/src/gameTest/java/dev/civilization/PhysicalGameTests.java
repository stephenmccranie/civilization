package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization") @PrefixGameTestTemplate(false)
public final class PhysicalGameTests {
    @GameTest(template="industrial") public static void geometryConservesMaterialAndCapacity(GameTestHelper h){
        var full=new BlockState[8];Arrays.fill(full,Blocks.OAK_PLANKS.defaultBlockState());
        var half=full.clone();for(int i=4;i<8;i++)half[i]=null;
        var a=PhysicalSample.of(full,false);var b=PhysicalSample.of(half,false);
        h.assertTrue(a.materialMassKg()==2*b.materialMassKg(),"Two halves equal one full block mass");
        double air=PhysicalMaterials.named("air").density()*PhysicalMaterials.named("air").specificHeat();
        h.assertTrue(Math.abs(2*b.capacityJPerK()-air-a.capacityJPerK())<.001,"Solid capacity conserved with vacant air accounted separately");
        double total=0;for(int i=0;i<8;i++){var eighth=new BlockState[8];eighth[i]=full[i];total+=PhysicalSample.of(eighth,false).materialMassKg();}
        h.assertTrue(total==a.materialMassKg(),"Eight eighths equal original mass");
        var mixed=new BlockState[8];mixed[0]=Blocks.OAK_PLANKS.defaultBlockState();mixed[1]=Blocks.IRON_BLOCK.defaultBlockState();
        h.assertTrue(PhysicalSample.of(mixed,false).materialMassKg()==(600+7800)/8d,"Mixed contents sum component masses");
        h.assertTrue(PhysicalSample.of(new BlockState[8],false).materialMassKg()==0,"Air adds no rigid material mass");h.succeed();
    }
    @GameTest(template="industrial") public static void cutFacesAndVanillaSlabsAgree(GameTestHelper h){
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));
        l.setBlockAndUpdate(p,Blocks.OAK_SLAB.defaultBlockState());var slab=PhysicalSample.at(l,p);
        h.assertTrue(slab.occupiedVolume()==.5&&slab.materialMassKg()==300,"Vanilla slab resolves wood and half volume");
        h.assertTrue(slab.faces().get(Direction.DOWN).solidArea()==1&&slab.faces().get(Direction.UP).solidArea()==0,"Orientation controls face coverage");
        l.setBlockAndUpdate(p,CuttingContent.PIECE.get().defaultBlockState());var cut=(CutBlockEntity)l.getBlockEntity(p);
        var cells=new BlockState[8];cells[0]=Blocks.OAK_PLANKS.defaultBlockState();cells[1]=Blocks.IRON_BLOCK.defaultBlockState();cut.cells(cells);
        var a=PhysicalSample.at(l,p);cells[2]=Blocks.OAK_PLANKS.defaultBlockState();cut.cells(cells);var b=PhysicalSample.at(l,p);
        h.assertTrue(b.materialMassKg()-a.materialMassKg()==75,"Contents edits cannot leave a stale mass cache");
        var face=a.faces().get(Direction.WEST);h.assertTrue(face.solidArea()==.25&&face.openArea()==.75,"Exposed quarter face and open paths retained");
        double k=face.throughConductance();h.assertTrue(k>0&&k<PhysicalMaterials.named("metal").conductivity(),"Layered resistance does not average metal through timber");h.succeed();
    }
    @GameTest(template="industrial") public static void sharedDataAndSableStaticDefinition(GameTestHelper h) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var l=h.getLevel();var p=h.absolutePos(new BlockPos(5,4,5));l.setBlockAndUpdate(p,Blocks.IRON_BLOCK.defaultBlockState());
        double expected=PhysicalMaterials.of(Blocks.IRON_BLOCK.defaultBlockState()).density();
        h.assertTrue(SablePhysicalBridge.mass(l,p)==expected,"Pinned Sable applies test-only static mass generated from shared material data");
        var definition=SablePhysicalBridge.definition("minecraft:iron_block",PhysicalMaterials.named("metal"));
        h.assertTrue(definition.getAsJsonObject("properties").get("sable:mass").getAsDouble()==expected,"Adapter export shares the same mass source");
        h.assertTrue(SablePhysicalBridge.address(l,p).vessel()==null,"Ordinary world block has world domain");
        int result=l.getServer().getCommands().getDispatcher().execute("civilization physics "+p.getX()+" "+p.getY()+" "+p.getZ(),l.getServer().createCommandSourceStack().withLevel(l));
        h.assertTrue(result==1,"Inspector executes against a loaded block");h.succeed();
    }
    @GameTest(template="industrial") public static void legacyThermalBalanceIsExplicit(GameTestHelper h){
        h.assertTrue(ThermalField.capacity(Blocks.OAK_PLANKS.defaultBlockState())==3&&ThermalField.conductance(Blocks.OAK_PLANKS.defaultBlockState())==.025,"Timber balance preserved");
        h.assertTrue(ThermalField.capacity(Blocks.BRICKS.defaultBlockState())==10&&ThermalField.conductance(Blocks.BRICKS.defaultBlockState())==.08,"Masonry balance preserved");
        h.assertTrue(ThermalField.capacity(Blocks.GLASS.defaultBlockState())==2&&ThermalField.conductance(Blocks.GLASS.defaultBlockState())==.3,"Glass legacy capacity is not its new physical capacity");
        var open=Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN,true);h.assertTrue(ThermalField.conductance(open)==1,"Door state still controls legacy air opening");
        h.assertTrue(ThermalField.conductance(IndustrialContent.CASING.get().defaultBlockState())==.6,"Casing keeps metal conductance");h.succeed();
    }

    @GameTest(template="industrial") public static void materialReloadIsAtomicAndUncached(GameTestHelper h) throws java.io.IOException {
        try(var in=PhysicalMaterials.class.getResourceAsStream("/data/civilization/physical_materials/defaults.json")){
            var original=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            try{
                var invalid=original.deepCopy();invalid.getAsJsonObject("profiles").getAsJsonObject("wood").addProperty("density_kg_m3",-1);
                boolean rejected=false;try{PhysicalMaterials.replace(invalid);}catch(IllegalArgumentException expected){rejected=true;}
                h.assertTrue(rejected&&PhysicalMaterials.of(Blocks.OAK_PLANKS.defaultBlockState()).density()==600,"Invalid reload preserves previous registry");
                var changed=original.deepCopy();changed.getAsJsonObject("profiles").getAsJsonObject("wood").addProperty("density_kg_m3",660);PhysicalMaterials.replace(changed);
                var cells=new BlockState[8];Arrays.fill(cells,Blocks.OAK_PLANKS.defaultBlockState());
                h.assertTrue(PhysicalSample.of(cells,false).materialMassKg()==660,"Material reload immediately updates derived values");
                changed.getAsJsonObject("blocks").addProperty("minecraft:oak_planks","metal");PhysicalMaterials.replace(changed);
                h.assertTrue(PhysicalMaterials.of(Blocks.OAK_PLANKS.defaultBlockState()).id().equals("metal"),"Exact selector wins over tag default");
            }finally{PhysicalMaterials.replace(original);}
        }
        h.succeed();
    }
}
