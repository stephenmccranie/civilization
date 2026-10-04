package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Exposed cell faces bake into the ordinary chunk mesh, including under Sodium. */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CoalMiningModel extends BakedModelWrapper<BakedModel> {
    private final Map<Long,List<BakedQuad>> cache=Collections.synchronizedMap(new LinkedHashMap<>(128,.75f,true){@Override protected boolean removeEldestEntry(Map.Entry<Long,List<BakedQuad>> e){return size()>512;}});
    public CoalMiningModel(BakedModel base){super(base);}
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult e){e.getModels().replaceAll((id,m)->id.id().getNamespace().equals("civilization")&&id.id().getPath().equals("coal_workface")?new CoalMiningModel(m):m);}
    @Override public List<BakedQuad> getQuads(BlockState s,Direction side,RandomSource r){return getQuads(s,side,r,ModelData.EMPTY,null);}
    @Override public List<BakedQuad> getQuads(BlockState s,Direction side,RandomSource r,ModelData data,RenderType type){
        if(side!=null)return List.of();long mask=data.has(CoalWorkfaceEntity.MASK)?data.get(CoalWorkfaceEntity.MASK):CoalGeometry.FULL;
        return cache.computeIfAbsent(mask,m->{
            var material=Blocks.COAL_BLOCK.defaultBlockState();var source=Minecraft.getInstance().getBlockRenderer().getBlockModel(material);var result=new ArrayList<BakedQuad>();
            for(int i=0;i<64;i++)if((m&(1L<<i))!=0){int x=i&3,y=(i>>2)&3,z=(i>>4)&3;
                for(var face:Direction.values())if(!CoalGeometry.occupied(m,x+face.getStepX(),y+face.getStepY(),z+face.getStepZ()))
                    for(var q:source.getQuads(material,face,RandomSource.create(0),ModelData.EMPTY,null))result.add(CutModel.crop(q,CoalGeometry.box(i)));
            }
            return List.copyOf(result);
        });
    }
}
