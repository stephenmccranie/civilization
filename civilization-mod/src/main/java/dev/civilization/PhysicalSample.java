package dev.civilization;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Material/geometry snapshot; inventory and fluid-tank contents are deliberately not inferred. */
public record PhysicalSample(double occupiedVolume,double materialMassKg,double capacityJPerK,
                             Map<Direction,Face> faces,Set<String> materials,boolean approximateGeometry) {
    public record Face(double solidArea,double openArea,double throughConductance) {}
    public PhysicalSample {faces=Map.copyOf(faces);materials=Set.copyOf(materials);}
    public static PhysicalSample at(BlockGetter level,BlockPos p){
        var state=level.getBlockState(p);var cells=new BlockState[8];boolean approximate=false;
        if(level.getBlockEntity(p) instanceof CutBlockEntity cut)cells=cut.cells().clone();
        else if(state.getBlock() instanceof SlabBlock) {
            var shape=state.getShape(level,p);cells=CutCells.filled(shape.bounds(),SlabIntegration.material(state));
        }else if(!state.isAir()){
            var shape=state.getShape(level,p);
            if(shape.isEmpty()&&state.getBlock() instanceof LiquidBlock){Arrays.fill(cells,state);approximate=true;}
            else if(!shape.isEmpty()){
                for(int i=0;i<8;i++){var c=CutCells.box(i).getCenter();for(var box:shape.toAabbs())if(box.contains(c)){cells[i]=state;break;}}
                // Arbitrary geometry uses half-grid occupancy; do not label it exact.
                approximate=!shape.equals(net.minecraft.world.phys.shapes.Shapes.block());
            }
        }
        return of(cells,approximate);
    }
    public static PhysicalSample of(BlockState[] cells,boolean approximate){
        if(cells.length!=8)throw new IllegalArgumentException("Expected eight half-grid cells");
        double v=0,m=0,c=0;var ids=new TreeSet<String>();
        for(var s:cells){var material=s==null||s.isAir()?PhysicalMaterials.named("air"):PhysicalMaterials.of(s);
            boolean occupied=s!=null&&!s.isAir();
            if(occupied){v+=.125;m+=material.density()*.125;ids.add(material.id());}
            c+=material.density()*material.specificHeat()*.125;
        }
        if(ids.isEmpty())ids.add("air");
        var faces=new EnumMap<Direction,Face>(Direction.class);
        for(var d:Direction.values()){
            int bit=switch(d.getAxis()){case X->1;case Y->2;case Z->4;};
            boolean positive=d.getAxisDirection()==Direction.AxisDirection.POSITIVE;
            double area=0,g=0;
            for(int i=0;i<8;i++)if(((i&bit)!=0)==positive){
                var a=cells[i];var b=cells[i^bit];
                if(a!=null&&!a.isAir())area+=.25;
                double ka=(a==null?PhysicalMaterials.named("air"):PhysicalMaterials.of(a)).conductivity();
                double kb=(b==null?PhysicalMaterials.named("air"):PhysicalMaterials.of(b)).conductivity();
                // Four parallel columns, each with two half-meter resistances in series.
                g+=.25/(.5/ka+.5/kb);
            }
            faces.put(d,new Face(area,1-area,g));
        }
        return new PhysicalSample(v,m,c,faces,ids,approximate);
    }
}
