package dev.civilization.client;

import dev.civilization.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Crops source UVs with the geometry. One model for arbitrary resource-pack materials. */
public final class CutModel extends BakedModelWrapper<BakedModel> {
    private final BlockState itemMaterial, itemShape;
    public CutModel(BakedModel base) { this(base, null, null); }
    private CutModel(BakedModel base, BlockState material, BlockState shape) { super(base); itemMaterial = material; itemShape = shape; }
    private BlockState material(ModelData data) { return itemMaterial != null ? itemMaterial : data.has(CutBlockEntity.MATERIAL) ? data.get(CutBlockEntity.MATERIAL) : Blocks.COBBLESTONE.defaultBlockState(); }
    private BakedModel source(BlockState state) { return Minecraft.getInstance().getBlockRenderer().getBlockModel(state); }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) { return getQuads(state, side, random, ModelData.EMPTY, null); }
    @Override public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type) {
        if (side != null) return List.of(); // Include all piece faces, even against a neighboring partial piece.
        if(itemShape==null && data.has(CutBlockEntity.CELLS)) {
            var cells=data.get(CutBlockEntity.CELLS);var result=new ArrayList<BakedQuad>();
            for(int i=0;i<8;i++)if(cells[i]!=null) {
                var material=cells[i];var model=source(material);
                if(type!=null && !model.getRenderTypes(material,random,ModelData.EMPTY).contains(type))continue;
                var box=CutCells.box(i);
                for(Direction face:Direction.values()) {
                    int nx=(i&1)+face.getStepX(),ny=((i>>1)&1)+face.getStepY(),nz=((i>>2)&1)+face.getStepZ();
                    if(nx>=0 && nx<2 && ny>=0 && ny<2 && nz>=0 && nz<2 && material.equals(cells[nx|ny<<1|nz<<2]))continue;
                    for(var quad:model.getQuads(material,face,random,data,type))result.add(cellTint(crop(quad,box),i));
                }
                for(var quad:model.getQuads(material,null,random,data,type))result.add(cellTint(crop(quad,box),i));
            }
            return result;
        }
        var shape = itemShape != null ? itemShape : state != null ? state : CuttingContent.PIECE.get().defaultBlockState();
        var material = material(data); if (material.is(CuttingContent.PIECE.get())) material = Blocks.COBBLESTONE.defaultBlockState();
        var source = source(material); var result = new ArrayList<BakedQuad>();
        for (Direction face : Direction.values()) for (var quad : source.getQuads(material, face, random, data, type)) result.add(crop(quad, shape));
        for (var quad : source.getQuads(material, null, random, data, type)) result.add(crop(quad, shape));
        return result;
    }
    private static BakedQuad crop(BakedQuad quad, BlockState state) {
        return crop(quad,CutBlock.bounds(state));
    }
    private static BakedQuad cellTint(BakedQuad quad,int cell) {return quad.isTinted()?new BakedQuad(quad.getVertices(),(cell+1)*256+quad.getTintIndex(),quad.getDirection(),quad.getSprite(),quad.isShade()):quad;}
    public static BakedQuad crop(BakedQuad quad, net.minecraft.world.phys.AABB box) {
        int[] original = quad.getVertices(), out = original.clone(); int stride = original.length / 4;
        double[] lows={box.minX,box.minY,box.minZ},sizes={box.getXsize(),box.getYsize(),box.getZsize()};
        for(int i=0;i<4;i++) for(int a=0;a<3;a++) {
            float old=Float.intBitsToFloat(original[i*stride+a]);float value=(float)(lows[a]+old*sizes[a]);
            out[i*stride+a]=Float.floatToRawIntBits(value);
            if(sizes[a]==1)continue;
            for(int j=0;j<4;j++) {
                float other=Float.intBitsToFloat(original[j*stride+a]);if(Math.abs(other-old)<.9)continue;
                boolean same=true;for(int b=0;b<3;b++)if(b!=a && original[i*stride+b]!=original[j*stride+b])same=false;
                if(!same)continue;
                float t=(value-old)/(other-old);
                for(int uv=4;uv<=5;uv++) {
                    float base=Float.intBitsToFloat(original[i*stride+uv]),neighbor=Float.intBitsToFloat(original[j*stride+uv]);
                    float current=Float.intBitsToFloat(out[i*stride+uv]);
                    out[i*stride+uv]=Float.floatToRawIntBits(current+(neighbor-base)*t);
                }
                break;
            }
        }
        return new BakedQuad(out, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }
    @Override public net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        if(data.has(CutBlockEntity.CELLS)) {
            var types=new java.util.HashSet<RenderType>();for(var cell:data.get(CutBlockEntity.CELLS))if(cell!=null)for(var type:source(cell).getRenderTypes(cell,random,ModelData.EMPTY))types.add(type);
            return net.neoforged.neoforge.client.ChunkRenderTypeSet.of(types);
        }
        var material=material(data); return source(material).getRenderTypes(material,random,ModelData.EMPTY);
    }
    @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(ModelData data) { return source(material(data)).getParticleIcon(ModelData.EMPTY); }
    @Override public List<BakedModel> getRenderPasses(net.minecraft.world.item.ItemStack stack, boolean fabulous) { return List.of(this); }
    @Override public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context, com.mojang.blaze3d.vertex.PoseStack pose, boolean leftHand) {
        originalModel.applyTransform(context, pose, leftHand);
        return this;
    }
    @Override public List<RenderType> getRenderTypes(net.minecraft.world.item.ItemStack stack, boolean fabulous) {
        return source(material(ModelData.EMPTY)).getRenderTypes(new net.minecraft.world.item.ItemStack(material(ModelData.EMPTY).getBlock()), fabulous);
    }
    @Override public ItemOverrides getOverrides() {
        return new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel base, net.minecraft.world.item.ItemStack stack, net.minecraft.client.multiplayer.ClientLevel level, net.minecraft.world.entity.LivingEntity entity, int seed) {
                return new CutModel(originalModel, CuttingContent.material(stack), CuttingContent.PIECE.get().defaultBlockState().setValue(CutBlock.UNITS, CuttingContent.units(stack)));
            }
        };
    }
}
