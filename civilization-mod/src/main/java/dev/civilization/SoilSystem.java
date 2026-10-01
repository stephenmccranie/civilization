package dev.civilization;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
/** One sparse attachment per farm chunk. Production only advances from the vanilla active-chunk tick. */
@EventBusSubscriber(modid="civilization")
public final class SoilSystem {
 public static BlockPos cropBase(net.minecraft.world.level.Level l,BlockPos p){var s=l.getBlockState(p);return s.is(Blocks.PITCHER_CROP)&&s.getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER?p.below():p;}
 public static String kind(BlockState s){
  if(s.is(Blocks.WHEAT)||s.is(FarmingContent.FERTILIZED_WHEAT.get()))return "wheat";
  if(s.is(Blocks.CARROTS))return "carrot";if(s.is(Blocks.POTATOES))return "potato";if(s.is(Blocks.BEETROOTS))return "beet";
  if(s.is(Blocks.MELON_STEM)||s.is(Blocks.ATTACHED_MELON_STEM))return "melon";
  if(s.is(Blocks.PUMPKIN_STEM)||s.is(Blocks.ATTACHED_PUMPKIN_STEM))return "pumpkin";
  if(s.is(Blocks.TORCHFLOWER_CROP)||s.is(Blocks.TORCHFLOWER))return "torch";
  if(s.is(Blocks.PITCHER_CROP))return "pitcher";return "";
 }
 private static int age(BlockState s){for(var e:s.getValues().entrySet())if(e.getKey().getName().equals("age")&&e.getValue() instanceof Integer n)return n;return -1;}
 public static boolean crop(BlockState s){return !kind(s).isEmpty();}
 public static boolean mature(BlockState s){if(s.getBlock() instanceof CropBlock c)return c.isMaxAge(s);return s.is(Blocks.TORCHFLOWER)||s.is(Blocks.PITCHER_CROP)&&s.getValue(PitcherCropBlock.AGE)==4;}
 public static SoilChunk.Soil observe(ServerLevel l,BlockPos p){if(!l.hasChunkAt(p)||!(l.getBlockState(p).getBlock() instanceof FarmBlock))return null;var chunk=l.getChunkAt(p);var data=chunk.getData(SoilChunk.TYPE);var existing=data.soils.get(p.asLong());if(existing!=null)return existing;var soil=new SoilChunk.Soil();data.soils.put(p.asLong(),soil);chunk.setUnsaved(true);return soil;}
 public static boolean randomTick(ServerLevel l,BlockPos p,BlockState state){
  if(state.getBlock() instanceof FarmBlock&&Geography.canFarm(l,p)){observe(l,p);return true;}
  if(crop(state)&&!state.is(Blocks.TORCHFLOWER)){observe(l,p.below());return true;}return false;
 }
 public static void changed(ServerLevel l,BlockPos p,BlockState old,BlockState state){
  // Player harvesting removes the block before its drop callback. Keep the old yield
  // until that callback, but never transfer it to a replacement fruit or crop.
  if((state.is(Blocks.MELON)||state.is(Blocks.PUMPKIN))&&!old.is(state.getBlock())){var c=l.getChunkAt(p);if(c.hasData(SoilChunk.TYPE)){c.getData(SoilChunk.TYPE).fruitYields.remove(p.asLong());c.setUnsaved(true);}}
  if(!(old.getBlock() instanceof FarmBlock)&&!(state.getBlock() instanceof FarmBlock)&&!crop(old)&&!crop(state))return;
  if(state.getBlock() instanceof FarmBlock){observe(l,p);return;}
  var chunk=l.getChunkAt(p);if(old.getBlock() instanceof FarmBlock&&chunk.hasData(SoilChunk.TYPE)){chunk.getData(SoilChunk.TYPE).soils.remove(p.asLong());chunk.setUnsaved(true);}
  if(!crop(old)&&!crop(state))return;
  var soil=observe(l,p.below());if(soil==null)return;
  if(!kind(old).equals(kind(state))||age(state)>=0&&age(old)>=0&&age(state)<age(old)||mature(old)&&!mature(state)||old.is(FarmingContent.FERTILIZED_WHEAT.get())&&state.is(Blocks.WHEAT)){
   if(!kind(state).isEmpty()){soil.harvested=false;soil.fertilized=false;}
   soil.crop=kind(state);soil.work=0;soil.target=target(l);soil.fruitMade=false;
  }
  if(state.is(FarmingContent.FERTILIZED_WHEAT.get()))soil.fertilized=true;
  chunk.setUnsaved(true);
 }
 private static double target(ServerLevel l){return WeatherConfig.GROW_SECONDS.get()*(.9+l.random.nextDouble()*.2);}
 public static void tick(ServerLevel l,LevelChunk c){if(!c.hasData(SoilChunk.TYPE))return;var d=c.getData(SoilChunk.TYPE);if(++d.activeTicks<20)return;d.activeTicks=0;advance(l,c,1);}
 /** Public for accelerated checks: seconds are active simulation time, not world elapsed time. */
 public static void advance(ServerLevel l,LevelChunk c,double seconds){
  var d=c.getData(SoilChunk.TYPE);var iterator=d.soils.entrySet().iterator();
  while(iterator.hasNext()){
   var entry=iterator.next();var p=BlockPos.of(entry.getKey());var state=l.getBlockState(p);if(!(state.getBlock() instanceof FarmBlock)){iterator.remove();continue;}
   var s=entry.getValue();if(!Geography.canFarm(l,p))continue;
   boolean rain=l.isRainingAt(p.above());double wet=rain?seconds:Math.min(seconds,s.water);
   s.water=Math.clamp(s.water+(rain?4*seconds:-seconds),0,7200);
   int moisture=s.water>0||rain?7:0;if(state.getValue(FarmBlock.MOISTURE)!=moisture)l.setBlock(p,state.setValue(FarmBlock.MOISTURE,moisture),2);
   grow(l,p,s,seconds*.5+wet*.5);
  }
  d.fruitYields.entrySet().removeIf(e->{var b=l.getBlockState(BlockPos.of(e.getKey()));return !b.is(Blocks.MELON)&&!b.is(Blocks.PUMPKIN);});
  c.setUnsaved(true);
 }
 private static void grow(ServerLevel l,BlockPos soil,SoilChunk.Soil s,double work){
  var p=soil.above();var state=l.getBlockState(p);String kind=kind(state);
  if(kind.isEmpty()){s.crop="";s.work=0;s.fruitMade=false;s.fertilized=false;return;}
  if(!s.crop.equals(kind)){s.crop=kind;s.work=0;s.target=target(l);s.fruitMade=false;}
  if(s.target<=0)s.target=target(l);
  if(state.is(FarmingContent.FERTILIZED_WHEAT.get()))s.fertilized=true;
  if(mature(state)||state.getBlock() instanceof AttachedStemBlock||l.getRawBrightness(p,0)<9||!state.canSurvive(l,p))return;
  s.work=Math.min(s.target,s.work+work);
  if(state.getBlock() instanceof CropBlock crop){int age=s.work>=s.target?crop.getMaxAge():(int)(s.work*crop.getMaxAge()/s.target);if(age>crop.getAge(state))l.setBlock(p,crop.getStateForAge(age),2);return;}
  if(state.is(Blocks.PITCHER_CROP)){
   if(state.getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER)return;
   int age=Math.min(4,(int)(s.work*4/s.target));if(age<=state.getValue(PitcherCropBlock.AGE))return;
   if(age>=3&&!l.isEmptyBlock(p.above())&&!l.getBlockState(p.above()).is(Blocks.PITCHER_CROP))return;
   var grown=state.setValue(PitcherCropBlock.AGE,age);l.setBlock(p,grown,2);if(age>=3)l.setBlock(p.above(),grown.setValue(PitcherCropBlock.HALF,DoubleBlockHalf.UPPER),3);return;
  }
  if(state.getBlock() instanceof StemBlock){
   int age=s.fruitMade?7:Math.min(7,(int)(s.work*8/s.target));if(age>state.getValue(StemBlock.AGE)){state=state.setValue(StemBlock.AGE,age);l.setBlock(p,state,2);}
   if(s.work<s.target)return;
   var directions=new ArrayList<Direction>();Direction.Plane.HORIZONTAL.forEach(directions::add);Collections.rotate(directions,l.random.nextInt(4));
   for(var direction:directions){var fruit=p.relative(direction);if(!l.hasChunkAt(fruit))continue;var ground=l.getBlockState(fruit.below());if(l.isEmptyBlock(fruit)&&(ground.getBlock() instanceof FarmBlock||ground.is(BlockTags.DIRT))&&CivicAccess.boundary(l,p,fruit)){
    l.setBlockAndUpdate(fruit,(kind.equals("melon")?Blocks.MELON:Blocks.PUMPKIN).defaultBlockState());
    l.getChunkAt(fruit).getData(SoilChunk.TYPE).fruitYields.put(fruit.asLong(),s.fertilized?3:1);l.getChunkAt(fruit).setUnsaved(true);
    l.setBlockAndUpdate(p,(kind.equals("melon")?Blocks.ATTACHED_MELON_STEM:Blocks.ATTACHED_PUMPKIN_STEM).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,direction));
    s.fertilized=false;s.fruitMade=true;s.work=0;s.target=target(l);return;
   }}
  }
 }
 public static boolean fertilize(ServerLevel l,BlockPos p){var state=l.getBlockState(p);if(!crop(state)||mature(state)||state.getBlock() instanceof AttachedStemBlock||!Geography.canFarm(l,p.below()))return false;var s=observe(l,p.below());if(s==null||s.fertilized)return false;s.fertilized=true;l.getChunkAt(p).setUnsaved(true);return true;}
 public static boolean fertilized(ServerLevel l,BlockPos p){var s=observe(l,p.below());return s!=null&&s.fertilized;}
 public static String describe(ServerLevel l,BlockPos p){var s=observe(l,p);if(s==null||!Geography.canFarm(l,p))return "";return (s.water>0?"Rain-soaked | 100% growth | ~"+(int)Math.ceil(s.water/60)+" min water":"River-fed | 50% growth")+(s.fertilized?" | Fertilized":"");}
 @SubscribeEvent public static void harvest(BlockDropsEvent e){
  var l=e.getLevel();var p=e.getPos();var state=e.getState();int multiplier=1;
  if(e.isCanceled()||!l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS))return;
  boolean fruit=state.is(Blocks.MELON)||state.is(Blocks.PUMPKIN);
  if(fruit){var c=l.getChunkAt(p);if(c.hasData(SoilChunk.TYPE)){var stored=c.getData(SoilChunk.TYPE).fruitYields.remove(p.asLong());if(stored!=null){multiplier=stored;c.setUnsaved(true);}}}
  else {if(!mature(state))return;var base=state.is(Blocks.PITCHER_CROP)&&state.getValue(PitcherCropBlock.HALF)==DoubleBlockHalf.UPPER?p.below():p;var s=observe(l,base.below());if(state.is(Blocks.PITCHER_CROP)&&s!=null){if(s.harvested){e.getDrops().clear();return;}s.harvested=true;l.getChunkAt(base).setUnsaved(true);}multiplier=state.is(FarmingContent.FERTILIZED_WHEAT.get())||s!=null&&s.fertilized?3:1;if(s!=null){s.fertilized=false;l.getChunkAt(base).setUnsaved(true);}}
  var out=new ArrayList<ItemStack>();String k=kind(state);
  switch(k){
   case "wheat"->{out.add(new ItemStack(Items.WHEAT,multiplier));out.add(new ItemStack(Items.WHEAT_SEEDS,2));}
   case "carrot"->out.add(new ItemStack(Items.CARROT,1+3*multiplier));
   case "potato"->out.add(new ItemStack(Items.POTATO,1+3*multiplier));
   case "beet"->{out.add(new ItemStack(Items.BEETROOT,multiplier));out.add(new ItemStack(Items.BEETROOT_SEEDS,2));}
   case "torch"->out.add(new ItemStack(Items.TORCHFLOWER,multiplier));
   case "pitcher"->{out.add(new ItemStack(Items.PITCHER_PLANT,multiplier));}
   default->{if(state.is(Blocks.MELON))out.add(new ItemStack(Items.MELON_SLICE,3*multiplier));else if(state.is(Blocks.PUMPKIN))out.add(new ItemStack(Items.PUMPKIN,multiplier));else return;}
  }
  // Keep a canceled/empty drop event empty; creative destruction never manufactures produce.
  if(e.getDrops().isEmpty()&&!state.is(Blocks.PITCHER_CROP))return;
  e.getDrops().clear();for(var item:out)e.getDrops().add(new ItemEntity(l,p.getX()+.5,p.getY()+.5,p.getZ()+.5,item));
 }
}
