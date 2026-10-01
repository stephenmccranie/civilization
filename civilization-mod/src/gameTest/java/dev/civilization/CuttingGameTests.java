package dev.civilization;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("civilization")
@PrefixGameTestTemplate(false)
public final class CuttingGameTests {
    private static CraftingInput input(ItemStack a, ItemStack b) { return CraftingInput.of(2,1,List.of(a,b)); }
    @GameTest(template="industrial") public static void vanillaSlabAcquisitionRequiresSaw(GameTestHelper h) {
        var recipes=h.getLevel().getRecipeManager();
        h.assertTrue(recipes.getAllRecipesFor(RecipeType.CRAFTING).stream().noneMatch(r ->
                r.id().getNamespace().equals("minecraft") && r.id().getPath().endsWith("_slab")),
                "No three-block vanilla slab crafting remains");
        h.assertTrue(recipes.getAllRecipesFor(RecipeType.STONECUTTING).stream().noneMatch(r ->
                r.id().getNamespace().equals("minecraft") && r.value().getResultItem(h.getLevel().registryAccess()).getItem()
                        instanceof net.minecraft.world.item.BlockItem item && item.getBlock() instanceof SlabBlock),
                "Stonecutter cannot bypass the saw for vanilla slabs");
        var planks=new ItemStack(Items.OAK_PLANKS);
        var ordinary=CraftingInput.of(3,1,List.of(planks.copy(),planks.copy(),planks.copy()));
        h.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING,ordinary,h.getLevel()).isEmpty(),
                "Three planks do not craft six slabs");
        h.assertTrue(recipes.byKey(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("civilization","street_pavers_slab")).isEmpty(),
                "Street Pavers also require the saw rather than a three-block slab recipe");
        var cut=input(CuttingContent.STONE_SAW.toStack(),planks);
        h.assertTrue(recipes.getRecipeFor(RecipeType.CRAFTING,cut,h.getLevel()).isPresent()
                && new CuttingRecipe(CraftingBookCategory.BUILDING).assemble(cut,h.getLevel().registryAccess()).is(Items.OAK_SLAB),
                "Saw still cuts planks into canonical oak slabs");
        var road=input(CuttingContent.STONE_SAW.toStack(),new ItemStack(RoadContent.PAVERS.get()));
        h.assertTrue(new CuttingRecipe(CraftingBookCategory.BUILDING).assemble(road,h.getLevel().registryAccess()).is(RoadContent.SLAB.asItem()),
                "Saw cuts Street Pavers into their native slabs");
        h.succeed();
    }
    @GameTest(template="empty") public static void halfWallsOccludeOnlyTheirSolidFaces(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(1,2,1));
        var wall=CutGeometry.state(new net.minecraft.world.phys.AABB(.5,0,0,1,1,1));
        level.setBlockAndUpdate(pos,wall);
        var cut=(CutBlockEntity)level.getBlockEntity(pos);
        cut.material(Blocks.BRICKS.defaultBlockState());
        h.assertTrue(level.getBlockState(pos).canOcclude() && level.getBlockState(pos).useShapeForLightOcclusion(),"Cut geometry participates in vanilla face lighting");
        h.assertTrue(net.minecraft.world.level.lighting.LightEngine.getLightBlockInto(level,level.getBlockState(pos),pos,Blocks.AIR.defaultBlockState(),pos.east(),Direction.EAST,1)==16,
                "A solid half wall blocks light through its covered face");
        h.assertTrue(net.minecraft.world.level.lighting.LightEngine.getLightBlockInto(level,level.getBlockState(pos),pos,Blocks.AIR.defaultBlockState(),pos.west(),Direction.WEST,1)==1,
                "The open half still lets light enter");
        cut.material(Blocks.GLASS.defaultBlockState());
        h.assertTrue(net.minecraft.world.level.lighting.LightEngine.getLightBlockInto(level,level.getBlockState(pos),pos,Blocks.AIR.defaultBlockState(),pos.east(),Direction.EAST,1)==1,
                "Cut glass does not become an opaque wall");
        var mixed=new net.minecraft.world.level.block.state.BlockState[8];
        for(int i=0;i<8;i++)mixed[i]=(i&1)==1?Blocks.BRICKS.defaultBlockState():Blocks.GLASS.defaultBlockState();
        cut.cells(mixed);
        h.assertTrue(net.minecraft.world.level.lighting.LightEngine.getLightBlockInto(level,level.getBlockState(pos),pos,Blocks.AIR.defaultBlockState(),pos.east(),Direction.EAST,1)==16,
                "Mixed assemblies block light only on opaque filled faces");
        h.assertTrue(net.minecraft.world.level.lighting.LightEngine.getLightBlockInto(level,level.getBlockState(pos),pos,Blocks.AIR.defaultBlockState(),pos.west(),Direction.WEST,1)==1,
                "Glass cells stay translucent in a mixed assembly");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void changingCutWallMaterialRelightsASealedRoom(GameTestHelper h) {
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(4,4,4));
        for(int x=-1;x<=3;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
            level.setBlockAndUpdate(origin.offset(x,y,z),y==0&&z==0&&x>=0&&x<=2?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(origin,Blocks.GLOWSTONE.defaultBlockState());
        var wall=origin.east();
        var behind=origin.east(2);
        h.runAfterDelay(10,()->{
            h.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,behind)>0,"Open tunnel receives light: source="+level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,origin)+" wall="+level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,wall)+" behind="+level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,behind));
            level.setBlockAndUpdate(wall,CutGeometry.state(new net.minecraft.world.phys.AABB(.5,0,0,1,1,1)));
            var cut=(CutBlockEntity)level.getBlockEntity(wall);
            cut.material(Blocks.GLASS.defaultBlockState());
            h.runAfterDelay(20,()->{
                h.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,behind)>0,"Cut glass passes light: wall="+level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,wall)+" behind="+level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,behind)+" shape="+level.getBlockState(wall).getOcclusionShape(level,wall));
                cut.material(Blocks.BRICKS.defaultBlockState());
                h.runAfterDelay(30,()->{
                    h.assertTrue(level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK,behind)==0,"Changing the same cut block to solid brick removes leaked light");
                    h.succeed();
                });
            });
        });
    }
    @GameTest(template="industrial") public static void cuttingConservesVolumeAndConsumesDurability(GameTestHelper h) {
        var recipe=new CuttingRecipe(CraftingBookCategory.BUILDING);
        var tool=CuttingContent.IRON_SAW.toStack();
        var grid=input(tool,new ItemStack(Items.BRICKS));
        h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,h.getLevel()).isPresent(),"Cutting recipe registered and discoverable");
        var halves=recipe.assemble(grid,h.getLevel().registryAccess());
        h.assertTrue(halves.getCount()==2 && CuttingContent.units(halves)==2 && CuttingContent.material(halves).is(Blocks.BRICKS),"One full block yields exactly two matching halves");
        h.assertTrue(halves.is(Items.BRICK_SLAB),"Existing vanilla slab item is the canonical half");
        var remaining=recipe.getRemainingItems(grid);
        h.assertTrue(remaining.get(0).getDamageValue()==1 && tool.getDamageValue()==0 && remaining.get(1).isEmpty(),"Saw remainder loses one durability without mutating inputs");
        var quarters=recipe.assemble(input(remaining.get(0),halves),h.getLevel().registryAccess());
        h.assertTrue(quarters.getCount()==2 && CuttingContent.units(quarters)==1,"One half yields two quarters");
        var eighths=recipe.assemble(input(tool,quarters),h.getLevel().registryAccess());
        h.assertTrue(eighths.getCount()==2 && CuttingContent.units(eighths)==3,"One beam cuts into two eighth cubes");
        h.assertTrue(!recipe.matches(input(tool,eighths),h.getLevel()),"Eighth cubes cannot be cut smaller");
        var dying=CuttingContent.STONE_SAW.toStack();dying.setDamageValue(dying.getMaxDamage()-1);
        var last=input(dying,new ItemStack(Items.COBBLESTONE));
        h.assertTrue(recipe.matches(last,h.getLevel()) && recipe.getRemainingItems(last).get(0).isEmpty(),"Last use produces pieces and breaks the saw");
        h.succeed();
    }
    @GameTest(template="industrial") public static void unsafeInputsAndWrongSawTiersAreRejected(GameTestHelper h) {
        var recipe=new CuttingRecipe(CraftingBookCategory.BUILDING);
        for(var item:List.of(Items.CHEST,Items.FURNACE,Items.BEDROCK,Items.TNT,Items.WATER_BUCKET,Items.TORCH))
            h.assertTrue(!recipe.matches(input(CuttingContent.DIAMOND_SAW.toStack(),new ItemStack(item)),h.getLevel()),"Cannot cut unsafe/non-cube input "+item);
        h.assertTrue(!recipe.matches(input(CuttingContent.IRON_SAW.toStack(),new ItemStack(Items.OBSIDIAN)),h.getLevel()),"Obsidian requires diamond saw");
        h.assertTrue(recipe.matches(input(CuttingContent.DIAMOND_SAW.toStack(),new ItemStack(Items.OBSIDIAN)),h.getLevel()),"Diamond saw cuts obsidian");
        h.succeed();
    }
    @GameTest(template="industrial") public static void cutPiecesPersistAndDropTheirMaterial(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,2,3));
        var state=CuttingContent.PIECE.get().defaultBlockState().setValue(CutBlock.UNITS,1).setValue(CutBlock.SIDE,Direction.EAST);
        h.getLevel().setBlockAndUpdate(pos,state);
        var cut=(CutBlockEntity)h.getLevel().getBlockEntity(pos);cut.material(Blocks.COPPER_BLOCK.defaultBlockState());
        var saved=cut.saveWithFullMetadata(h.getLevel().registryAccess());
        var loaded=new CutBlockEntity(pos,state);loaded.setLevel(h.getLevel());loaded.loadWithComponents(saved,h.getLevel().registryAccess());h.getLevel().setBlockEntity(loaded);
        h.assertTrue(loaded.material().is(Blocks.COPPER_BLOCK),"Material survives persistence");
        var picked=state.getBlock().getCloneItemStack(h.getLevel(),pos,state);
        h.assertTrue(CuttingContent.material(picked).is(Blocks.COPPER_BLOCK) && CuttingContent.units(picked)==1,"Pick block retains material and volume");
        var drops=Block.getDrops(state,h.getLevel(),pos,loaded);
        h.assertTrue(drops.size()==1 && drops.getFirst().getCount()==1 && ItemStack.isSameItemSameComponents(drops.getFirst(),picked),"Breaking returns exactly this material piece");
        for(var face:Direction.values()) for(int units:new int[]{1,2,3}) {
            var shape=state.setValue(CutBlock.SIDE,face).setValue(CutBlock.UNITS,units).getShape(h.getLevel(),pos).bounds();
            h.assertTrue(Math.abs(shape.getXsize()*shape.getYsize()*shape.getZsize()-CuttingContent.volume(units))<.0001,"Collision volume correct for "+face);
            h.assertTrue(CutBlock.orientation(face,.5,.5,.5,false)==face.getOpposite(),"Face-center attachment correct for "+face);
            h.assertTrue(CutBlock.orientation(face,.01,.01,.01,true)==face.getOpposite(),"Crouch forces face attachment");
        }
        h.assertTrue(CutBlock.orientation(Direction.UP,.02,1,.5,false)==Direction.WEST,"Top-face edge selects vertical slab");
        h.succeed();
    }
    @GameTest(template="industrial") public static void joiningPiecesConsumesOnlyOneItemAndRestoresVolume(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(4,2,4));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"CutTest"));
        player.setPos(pos.getX()+3,pos.getY(),pos.getZ()+3);
        for(int units:new int[]{1,2,3}) {
            h.getLevel().setBlockAndUpdate(pos,CuttingContent.PIECE.get().defaultBlockState().setValue(CutBlock.UNITS,units));
            ((CutBlockEntity)h.getLevel().getBlockEntity(pos)).material(Blocks.BRICKS.defaultBlockState());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),units,2));
            var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.25,pos.getY()+.5,pos.getZ()+.25),Direction.UP,pos,false);
            var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
            player.getMainHandItem().useOn(context);
            var after=h.getLevel().getBlockState(pos);
            h.assertTrue(units==2 ? after.is(Blocks.BRICKS) : after.is(CuttingContent.PIECE.get()) && after.getValue(CutBlock.UNITS)==(units==3?1:2),"Joining restores the next larger volume");
            h.assertTrue(player.getMainHandItem().getCount()==1,"Joining consumes exactly one piece");
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void shapedMachineRequiresCorrectOrientationAndMaterial(GameTestHelper h) {
        var controller=h.absolutePos(new BlockPos(5,1,5));
        h.getLevel().setBlockAndUpdate(controller,KilnContent.KILN.get().defaultBlockState());
        KilnGameTests.buildShell(h,controller,Direction.NORTH,false);
        var part=MachineStructure.parts(false).stream().filter(p->p.units()==2).findFirst().orElseThrow();
        var pos=MachineStructure.position(controller,Direction.NORTH,part);var good=h.getLevel().getBlockState(pos);
        h.getLevel().setBlockAndUpdate(pos,good.setValue(CutBlock.SIDE,good.getValue(CutBlock.SIDE).getOpposite()));
        h.assertTrue(MachineStructure.check(h.getLevel(),controller,Direction.NORTH).status()==MachineStructure.INCOMPLETE,"Wrong-facing slab cannot form machine");
        h.getLevel().setBlockAndUpdate(pos,good);((CutBlockEntity)h.getLevel().getBlockEntity(pos)).material(Blocks.OAK_PLANKS.defaultBlockState());
        h.assertTrue(MachineStructure.check(h.getLevel(),controller,Direction.NORTH).status()==MachineStructure.INCOMPLETE,"Wrong material cannot form machine");
        ((CutBlockEntity)h.getLevel().getBlockEntity(pos)).material(Blocks.COBBLESTONE.defaultBlockState());
        h.assertTrue(MachineStructure.check(h.getLevel(),controller,Direction.NORTH).status()==MachineStructure.COMPLETE,"Correct repair forms machine");
        h.succeed();
    }
    @GameTest(template="industrial") public static void gridHasExactlySixSlabsTwelveBeamsEightCubes(GameTestHelper h) {
        for(int units:new int[]{2,1,3}) {
            var boxes=new java.util.HashSet<net.minecraft.world.phys.AABB>();
            for(var side:Direction.values())for(int corner=0;corner<4;corner++) {
                var box=CutGeometry.bounds(units,side,corner);boxes.add(box);
                for(double dimension:new double[]{box.getXsize(),box.getYsize(),box.getZsize()})h.assertTrue(dimension==.5 || dimension==1,"Every dimension belongs to half-block grid");
                var state=CutGeometry.state(box);h.assertTrue(CutBlock.bounds(state).equals(box),"Geometry round trip");
                var rotated=box;for(int turn=0;turn<4;turn++)rotated=CutGeometry.rotate(rotated,Rotation.CLOCKWISE_90);
                h.assertTrue(rotated.equals(box),"Four rotations preserve shape");
            }
            h.assertTrue(boxes.size()==(units==2?6:units==1?12:8),"Exact orientation count for stage "+units);
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void vanillaSlabsPlaceHorizontallyVerticallyAndJoin(GameTestHelper h) {
        for(var block:net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if(block instanceof SlabBlock && net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("minecraft"))
                h.assertTrue(SlabIntegration.base(block)!=null,"Vanilla slab integrated: "+block);
        }
        var pos=h.absolutePos(new BlockPos(4,2,4));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"SlabTest"));
        player.setPos(pos.getX()+3,pos.getY(),pos.getZ()+3);
        h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BRICK_SLAB,4));
        var center=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5),Direction.UP,pos.below(),false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,center));
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.BRICK_SLAB),"Horizontal placement retains vanilla slab block");
        var top=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5),Direction.UP,pos,false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,top));
        h.assertTrue(h.getLevel().getBlockState(pos).is(Blocks.BRICKS),"Two vanilla slab items join to original full block");
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var edge=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.04,pos.getY(),pos.getZ()+.5),Direction.UP,pos.below(),false);
        player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,edge));
        var vertical=h.getLevel().getBlockState(pos);
        h.assertTrue(vertical.is(CuttingContent.PIECE.get()) && CutBlock.bounds(vertical).getXsize()==.5,"Same vanilla slab item places vertically");
        h.assertTrue(vertical.getBlock().getCloneItemStack(h.getLevel(),pos,vertical).is(Items.BRICK_SLAB),"Vertical slab picks as vanilla item");
        h.assertTrue(player.getMainHandItem().getCount()==1,"Each placement consumes exactly one slab");
        h.succeed();
    }
    @GameTest(template="industrial") public static void eighthCubeJoinsAlongAnyAxis(GameTestHelper h) {
        for(var side:Direction.values()) {
            var b=CutGeometry.bounds(3,side,0);var open=side.getOpposite();var joined=CutGeometry.joined(b,open);
            h.assertTrue(joined!=null && joined.getXsize()*joined.getYsize()*joined.getZsize()==.25,"Eighths join to beam along "+open);
            h.assertTrue(CutGeometry.state(joined).getValue(CutBlock.UNITS)==1,"Joined eighths identify as beam");
        }
        h.succeed();
    }
    @GameTest(template="industrial") public static void nativeAndCustomPiecesRetainWater(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,2,3));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"WetCutTest"));
        player.setPos(pos.getX()+3,pos.getY(),pos.getZ()+3);
        h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        for(int units:new int[]{2,1,3}) {
            h.getLevel().setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,CuttingContent.stack(Blocks.BRICKS.defaultBlockState(),units,1));
            var hit=new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY(),pos.getZ()+.5),Direction.UP,pos.below(),false);
            player.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
            var placed=h.getLevel().getBlockState(pos);
            h.assertTrue(placed.hasProperty(CutBlock.WATERLOGGED) && placed.getValue(CutBlock.WATERLOGGED),"Placement retains water for stage "+units);
            h.assertTrue(placed.getFluidState().is(net.minecraft.tags.FluidTags.WATER),"Water remains real fluid");
        }
        h.succeed();
    }
}
