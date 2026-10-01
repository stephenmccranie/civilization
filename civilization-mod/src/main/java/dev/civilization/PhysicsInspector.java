package dev.civilization;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="civilization")
public final class PhysicsInspector {
    @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("civilization").then(Commands.literal("physics").requires(s->s.hasPermission(2))
            .executes(c->{var p=c.getSource().getPlayerOrException();var hit=p.pick(8,0,false);if(!(hit instanceof BlockHitResult b)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK){c.getSource().sendFailure(Component.literal("Look at a block within eight blocks."));return 0;}return inspect(c.getSource(),b.getBlockPos());})
            .then(Commands.argument("pos",BlockPosArgument.blockPos()).executes(c->inspect(c.getSource(),BlockPosArgument.getLoadedBlockPos(c,"pos"))))));
    }
    private static int inspect(CommandSourceStack source,BlockPos pos){
        var level=source.getLevel();if(!level.hasChunkAt(pos)){source.sendFailure(Component.literal("Block must be loaded."));return 0;}
        try{
            var at=SablePhysicalBridge.address(level,pos);var sample=PhysicalSample.at(level,pos);var state=level.getBlockState(pos);
            source.sendSuccess(()->Component.literal(String.format(java.util.Locale.ROOT,
                "Materials %s%s | Volume %.3f m³ | Material mass %.1f kg | Capacity %.0f J/K | Sable mass %.1f (engine units)",
                String.join("/",sample.materials()),sample.approximateGeometry()?" (approx. shape)":"",sample.occupiedVolume(),sample.materialMassKg(),sample.capacityJPerK(),SablePhysicalBridge.mass(level,pos))),false);
            source.sendSuccess(()->Component.literal("Space: "+at.dimension()+" / "+(at.vessel()==null?"world":"vessel "+at.vessel())+" | local "+pos.toShortString()+" | world "+at.world()),false);
            source.sendSuccess(()->Component.literal(String.format(java.util.Locale.ROOT,"Legacy heat capacity %.2f / conductance %.3f; physical values are not yet applied to vehicle handling or heat saves. Inventory/tank contents excluded.",ThermalField.capacity(state),ThermalField.conductance(state))),false);
            for(var face:sample.faces().entrySet())source.sendSuccess(()->Component.literal(String.format(java.util.Locale.ROOT,"%s: solid face %.2f m² | open %.2f m² | through-conductance %.3f W/K",face.getKey(),face.getValue().solidArea(),face.getValue().openArea(),face.getValue().throughConductance())),false);
            return 1;
        }catch(IllegalArgumentException ex){source.sendFailure(Component.literal(ex.getMessage()));return 0;}
    }
}
