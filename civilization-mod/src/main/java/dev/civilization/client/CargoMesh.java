package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.civilization.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * A store's goods drawn as its {@link CargoLayout} places them, built once per layout: each stored block a quarter-size
 * block, eight pieces for a cell of slabs or cut pieces, in the grid's frame from its first-row, first-column, bottom
 * corner. Goods drawn from their block model become block quads by render type, as their model draws them among the
 * other goods (see {@link Goods}), without the faces hidden between whole cells of solid blocks and with their tint baked
 * in, so the same quads draw in the terrain or on a moving store. Goods drawn by a block entity or as their item, such as
 * chests and the Baking Oven, are {@link #drawn} each frame instead.
 */
@EventBusSubscriber(modid="civilization",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class CargoMesh {
    record Cell(BlockState state,float x,float y,float z,float width,float height){}
    private static final Direction[] SIDES={Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,null};
    private static final Map<CargoLayout,CargoMesh> BUILT=Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<RenderType,List<BakedQuad>> quads;
    final List<Cell> drawn;
    private CargoMesh(Map<RenderType,List<BakedQuad>> quads,List<Cell> drawn){this.quads=quads;this.drawn=drawn;}

    /** The goods of `layout`; built on first use, by whichever thread draws them first. */
    static CargoMesh of(CargoLayout layout){return BUILT.computeIfAbsent(layout,CargoMesh::build);}
    /** Meshes hold the block atlas's sprites, which a resource reload replaces. */
    @SubscribeEvent static void reload(ModelEvent.BakingCompleted e){BUILT.clear();}

    Set<RenderType> renderTypes(){return quads.keySet();}
    List<BakedQuad> quads(RenderType type){return quads.getOrDefault(type,List.of());}

    /** Draws everything on a moving store, with the grid's corner at the pose origin. */
    void render(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        quads.forEach((type,list)->{
            var consumer=buffers.getBuffer(RenderTypeHelper.getEntityRenderType(type,false));
            for(var quad:list)consumer.putBulkData(pose.last(),quad,1,1,1,1,light,overlay,true);
        });
        renderDrawn(pose,buffers,light,overlay);
    }

    /** Draws the goods that have no block quads, with the grid's corner at the pose origin. */
    void renderDrawn(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        for(var c:drawn){
            pose.pushPose();pose.translate(c.x,c.y,c.z);pose.scale(c.width,c.height,c.width);
            if(c.state.getRenderShape()==RenderShape.INVISIBLE){
                pose.translate(.5f,.5f,.5f);
                Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(c.state.getBlock()),ItemDisplayContext.NONE,light,overlay,pose,buffers,null,0);
            }else Minecraft.getInstance().getBlockRenderer().renderSingleBlock(c.state,pose,buffers,light,overlay);
            pose.popPose();
        }
    }

    private static CargoMesh build(CargoLayout layout){
        var quads=new HashMap<RenderType,List<BakedQuad>>();var drawn=new ArrayList<Cell>();
        var goods=new Goods(layout);float width=CargoGrid.CELL,height=layout.grid.height();
        for(int cell=0;cell<layout.cells();cell++){
            var at=goods.pos(cell);
            float x=at.getX()*width,y=at.getY()*height,z=at.getZ()*width;
            var whole=layout.whole(cell);
            if(whole!=null)add(whole,goods,at,x,y,z,width,height,side->solid(goods,at.relative(side)),quads,drawn);
            else for(int k=0;k<8&&cell*8+k<layout.eighths.size();k++)
                add(layout.eighths.get(cell*8+k),goods,at,x+k%2*width/2,y+k/4*height/2,z+k/2%2*width/2,width/2,height/2,side->false,quads,drawn);
        }
        return new CargoMesh(quads,drawn);
    }

    private static boolean solid(Goods goods,BlockPos pos){return goods.getBlockState(pos).isSolidRender(goods,pos);}

    /** `state` at `at` among the goods, drawn into the box from `x`, `y` and `z` of `width` by `height` by `width`. */
    private static void add(BlockState state,Goods goods,BlockPos at,float x,float y,float z,float width,float height,java.util.function.Predicate<Direction> hidden,Map<RenderType,List<BakedQuad>> quads,List<Cell> drawn){
        if(state.getRenderShape()!=RenderShape.MODEL){drawn.add(new Cell(state,x,y,z,width,height));return;}
        var model=Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        var data=model.getModelData(goods,at,state,ModelData.EMPTY);long seed=state.getSeed(at);var random=RandomSource.create(seed);
        for(var type:model.getRenderTypes(state,random,data)){
            var into=quads.computeIfAbsent(type,t->new ArrayList<>());
            for(var side:SIDES){
                if(side!=null&&hidden.test(side))continue;
                random.setSeed(seed);
                for(var quad:model.getQuads(state,side,random,data,type))
                    into.add(place(quad,x,y,z,width,height,quad.isTinted()?Minecraft.getInstance().getBlockColors().getColor(state,null,null,quad.getTintIndex()):-1));
            }
        }
    }

    /**
     * The goods' whole blocks as a world of their own, each cell at its column, layer and row, and air around them, so a
     * model that reads its surroundings, such as a resource pack's connected, repeating or random textures, draws a
     * stored block as it would among those neighbours. Light, shade and tint come from the world the goods are drawn in.
     */
    private record Goods(CargoLayout layout) implements BlockAndTintGetter {
        BlockPos pos(int cell){var grid=layout.grid;return new BlockPos(cell%grid.across(),cell/grid.layer(),cell%grid.layer()/grid.across());}
        @Override public BlockState getBlockState(BlockPos pos){
            var grid=layout.grid;
            var inside=pos.getX()>=0&&pos.getX()<grid.across()&&pos.getZ()>=0&&pos.getZ()<grid.along()&&pos.getY()>=0;
            var state=inside?layout.whole(pos.getY()*grid.layer()+pos.getZ()*grid.across()+pos.getX()):null;
            return state==null?Blocks.AIR.defaultBlockState():state;
        }
        @Override public FluidState getFluidState(BlockPos pos){return getBlockState(pos).getFluidState();}
        @Override public BlockEntity getBlockEntity(BlockPos pos){return null;}
        @Override public int getHeight(){return layout.grid.layers();}
        @Override public int getMinBuildHeight(){return 0;}
        @Override public float getShade(Direction side,boolean shade){return Minecraft.getInstance().level.getShade(side,shade);}
        @Override public LevelLightEngine getLightEngine(){return Minecraft.getInstance().level.getLightEngine();}
        @Override public int getBlockTint(BlockPos pos,ColorResolver resolver){return Minecraft.getInstance().level.getBlockTint(pos,resolver);}
    }

    /** `quad` scaled by `width`, `height` and `width` and moved by `x`, `y` and `z`, its colour multiplied by `tint`. */
    static BakedQuad place(BakedQuad quad,float x,float y,float z,float width,float height,int tint){
        var v=quad.getVertices().clone();int stride=v.length/4;
        for(int i=0;i<4;i++){
            int o=i*stride;
            v[o]=Float.floatToRawIntBits(x+Float.intBitsToFloat(v[o])*width);
            v[o+1]=Float.floatToRawIntBits(y+Float.intBitsToFloat(v[o+1])*height);
            v[o+2]=Float.floatToRawIntBits(z+Float.intBitsToFloat(v[o+2])*width);
            if(tint!=-1)v[o+3]=multiply(v[o+3],tint);
        }
        return new BakedQuad(v,-1,quad.getDirection(),quad.getSprite(),quad.isShade(),false);
    }

    /** A vertex colour (alpha, blue, green, red from the high byte) times an RGB tint. */
    private static int multiply(int abgr,int rgb){
        int r=(abgr&0xFF)*(rgb>>16&0xFF)/255,g=(abgr>>8&0xFF)*(rgb>>8&0xFF)/255,b=(abgr>>16&0xFF)*(rgb&0xFF)/255;
        return abgr&0xFF000000|b<<16|g<<8|r;
    }
}
