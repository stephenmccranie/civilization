package dev.civilization;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;

/**
 * One machine drawn as a native model and built in sections: what each section costs, the invisible collision cells its
 * cubes fill and the ports it places, read once from the definition its Blockbench source exports.
 */
public final class MachineModel {
    public static final MachineModel OIL_DERRICK=load("oil_derrick"), KILN=load("kiln"), FOUNDRY=load("foundry"), FERTILIZER_FURNACE=load("fertilizer_furnace"), TANNERY=load("tannery"), TEXTILE=load("textile_workshop"), SMITHY=load("smithy"), OIL_ENGINE=load("oil_engine"), COAL_ENGINE=load("coal_engine"), FIRED_HEATER=load("fired_heater"), DISTILLATION_COLUMN=load("distillation_column"), CONDENSER=load("condenser");

    /** Accepts any listed item or tag; `material` names them together on the guide. */
    public record Section(String name,List<String> items,String material,int count,int labor) {
        public boolean accepts(ItemStack stack){
            if(stack.isEmpty())return false;
            var id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            for(var item:items)if(item.startsWith("#")?stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(item.substring(1)))):item.equals(id))return true;
            return false;
        }
        /** A stack this section accepts, for creative fixtures. */
        public ItemStack sample(){
            for(var item:items)if(!item.startsWith("#"))return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)),count);
            var tag=TagKey.create(Registries.ITEM,ResourceLocation.parse(items.getFirst().substring(1)));
            return new ItemStack(BuiltInRegistries.ITEM.getTag(tag).orElseThrow().iterator().next().value(),count);
        }
    }
    /** A functional block the section places, such as a refinery port. */
    public record Port(int section,MachineStructure.Part part) {}
    public static final class Cell {
        private final int x,y,z,sectionMask;
        private final VoxelShape[] shapes;
        private final Map<Integer,VoxelShape[]> pieces;
        // Shapes per subset of this cell's own sections; no controller or world references, so shared by both sides.
        private final Map<Long,VoxelShape> partial=new java.util.concurrent.ConcurrentHashMap<>();
        Cell(int x,int y,int z,VoxelShape[] shapes,Map<Integer,VoxelShape[]> pieces){
            this.x=x;this.y=y;this.z=z;this.shapes=shapes;this.pieces=Collections.unmodifiableMap(new TreeMap<>(pieces));
            int bits=0;for(int part:pieces.keySet())bits|=1<<part;sectionMask=bits;
        }
        public int x(){return x;}public int y(){return y;}public int z(){return z;}
        /** The sections with pieces in this cell, one bit each. */
        public int sectionMask(){return sectionMask;}
        public Map<Integer,VoxelShape[]> pieces(){return pieces;}
        public VoxelShape shape(Direction front){return shapes[front.get2DDataValue()];}
        public VoxelShape shape(Direction front,int built){
            int active=built&sectionMask,index=front.get2DDataValue();
            if(active==sectionMask)return shapes[index];
            if(active==0)return Shapes.empty();
            if(Integer.bitCount(active)==1)return pieces.get(Integer.numberOfTrailingZeros(active))[index];
            return partial.computeIfAbsent((long)active<<2|index,key->{
                var result=Shapes.empty();for(var e:pieces.entrySet())if((active&(1<<e.getKey()))!=0)result=Shapes.or(result,e.getValue()[index]);return result;
            });
        }
    }

    /** `like` names the vanilla block whose sound, hardness and tool the part cells copy. */
    public final String id,geo,partBlock,like,animation;
    /** Whether the machine's fire warms its whole body, as a masonry kiln's does, rather than only its controller; its
     * lit bones are that fire and glow. */
    public final boolean heated;
    public final List<Section> sections;
    public final List<Cell> cells;
    public final List<Port> ports;
    /** Horizontal reach and vertical span of the cells around the controller, for culling and guides. */
    public final int reach,bottom,top;
    public final int all;
    /** The part cell that draws the machine's water, beside the controller so it redraws whenever the controller's lit
     * state changes, and drawing nothing else; -1 for a machine without water. */
    public final int water;
    public final IntegerProperty cell;

    private MachineModel(String id,JsonObject o){
        this.id=id;geo=o.get("geo").getAsString();partBlock=o.get("part").getAsString();like=o.get("like").getAsString();animation=o.has("animation")?o.get("animation").getAsString():null;heated=o.has("heated")&&o.get("heated").getAsBoolean();
        var sections=new ArrayList<Section>();
        for(var e:o.getAsJsonArray("sections")){
            var s=e.getAsJsonObject();var items=new ArrayList<String>();for(var item:s.getAsJsonArray("items"))items.add(item.getAsString());
            int count=s.get("count").getAsInt();
            // A section is paid from one held stack, and a hand carries building material an armful at a time.
            if(count<1||count>BlockCarry.LIMIT)throw new IllegalStateException(id+": "+s.get("name").getAsString()+" must cost one armful or less");
            sections.add(new Section(s.get("name").getAsString(),List.copyOf(items),s.get("material").getAsString(),count,s.has("labor")?s.get("labor").getAsInt():count));
        }
        if(sections.isEmpty()||sections.size()>31)throw new IllegalStateException(id+": a machine has 1 to 31 sections");
        this.sections=List.copyOf(sections);all=(1<<sections.size())-1;
        var ports=new ArrayList<Port>();
        if(o.has("ports"))for(var e:o.getAsJsonArray("ports")){
            var p=e.getAsJsonObject();
            ports.add(new Port(p.get("section").getAsInt(),new MachineStructure.Part(p.get("x").getAsInt(),p.get("y").getAsInt(),p.get("depth").getAsInt(),p.get("role").getAsString(),4,Direction.byName(p.get("side").getAsString()))));
        }
        this.ports=List.copyOf(ports);
        var cells=new ArrayList<Cell>();int reach=0,bottom=0,top=1;
        for(var e:o.getAsJsonArray("cells")){
            var c=e.getAsJsonObject();var offset=c.getAsJsonArray("offset");int x=offset.get(0).getAsInt(),y=offset.get(1).getAsInt(),z=offset.get(2).getAsInt();
            var shapes=new VoxelShape[4];var pieces=new TreeMap<Integer,VoxelShape[]>();
            for(var front:Direction.Plane.HORIZONTAL){
                var boxes=new ArrayList<VoxelShape>();
                for(var b:c.getAsJsonArray("boxes")){
                    var a=b.getAsJsonArray();int part=a.get(0).getAsInt();
                    if(part<0||part>=sections.size())throw new IllegalStateException(id+": cell box names a missing section");
                    double x0=a.get(1).getAsDouble(),y0=a.get(2).getAsDouble(),z0=a.get(3).getAsDouble(),x1=a.get(4).getAsDouble(),y1=a.get(5).getAsDouble(),z1=a.get(6).getAsDouble();
                    var box=switch(front){case EAST->Shapes.box(1-z1,y0,x0,1-z0,y1,x1);case SOUTH->Shapes.box(1-x1,y0,1-z1,1-x0,y1,1-z0);case WEST->Shapes.box(z0,y0,1-x1,z1,y1,1-x0);default->Shapes.box(x0,y0,z0,x1,y1,z1);};
                    boxes.add(box);var parts=pieces.computeIfAbsent(part,k->new VoxelShape[]{Shapes.empty(),Shapes.empty(),Shapes.empty(),Shapes.empty()});
                    parts[front.get2DDataValue()]=Shapes.or(parts[front.get2DDataValue()],box);
                }
                shapes[front.get2DDataValue()]=Shapes.or(Shapes.empty(),boxes.toArray(VoxelShape[]::new));
            }
            cells.add(new Cell(x,y,z,shapes,pieces));reach=Math.max(reach,Math.max(Math.abs(x),Math.abs(z))+1);bottom=Math.min(bottom,y);top=Math.max(top,y+1);
        }
        this.cells=List.copyOf(cells);this.reach=reach;this.bottom=bottom;this.top=top;
        cell=IntegerProperty.create("cell",0,Math.max(1,cells.size()-1));
        int host=-1;
        if(o.has("water")){
            var at=o.getAsJsonArray("water");int x=at.get(0).getAsInt(),y=at.get(1).getAsInt(),z=at.get(2).getAsInt();
            for(int i=0;i<this.cells.size();i++){var c=this.cells.get(i);if(c.x==x&&c.y==y&&c.z==z)host=i;}
            if(host<0||Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)))>1)throw new IllegalStateException(id+": water must be drawn by a part cell beside the controller");
        }
        water=host;
    }

    private static MachineModel load(String id){
        try(var in=MachineModel.class.getResourceAsStream("/assets/civilization/machines/"+id+".json")){
            if(in==null)throw new IllegalStateException("Missing machine definition "+id);
            return new MachineModel(id,JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject());
        }catch(IOException e){throw new ExceptionInInitializerError(e);}
    }
    public static List<MachineModel> all(){return List.of(OIL_DERRICK,KILN,FOUNDRY,FERTILIZER_FURNACE,TANNERY,TEXTILE,SMITHY,OIL_ENGINE,COAL_ENGINE,FIRED_HEATER,DISTILLATION_COLUMN,CONDENSER);}
    public Section section(int part){return sections.get(part);}
    /** Everything the model can occupy around a controller at `at`, in any facing. */
    public AABB area(net.minecraft.core.BlockPos at){return new AABB(at).inflate(reach,0,reach).setMinY(at.getY()+bottom).setMaxY(at.getY()+top);}
}
