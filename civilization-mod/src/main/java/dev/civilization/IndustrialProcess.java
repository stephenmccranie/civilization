package dev.civilization;

import net.minecraft.world.level.material.Fluid;

/** Fixed refinery batches shared by production and recipe viewers. */
public record IndustrialProcess(Fluid input,int inputMb,Fluid output,int outputMb,int oilMb,int sulfur,int ticks) {
    public static IndustrialProcess forKind(IndustrialBlock.Kind kind) {
        return switch(kind) {
            case REFINERY -> new IndustrialProcess(IndustrialContent.CRUDE.get(),250,IndustrialContent.HEATED.get(),250,0,0,100);
            case COLUMN -> new IndustrialProcess(IndustrialContent.HEATED.get(),1000,IndustrialContent.VAPOR.get(),800,200,1,400);
            case CONDENSER -> new IndustrialProcess(IndustrialContent.VAPOR.get(),200,IndustrialContent.FUEL.get(),200,0,0,100);
            default -> throw new IllegalArgumentException("Not a refinery stage: "+kind);
        };
    }
}
