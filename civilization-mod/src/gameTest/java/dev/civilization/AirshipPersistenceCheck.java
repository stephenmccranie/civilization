package dev.civilization;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.nio.file.Files;

@EventBusSubscriber(modid="civilization")
public final class AirshipPersistenceCheck {
    private static final String PHASE=System.getProperty("civilization.airshipPhase","manual");
    private static int ticks;
    @SubscribeEvent public static void start(ServerStartedEvent e){ticks=0;if(PHASE.equals("manual"))return;for(int x=0;x<3;x++)for(int z=0;z<3;z++){e.getServer().overworld().setChunkForced(x,z,true);e.getServer().overworld().getChunk(x,z);}}
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(PHASE.equals("manual")||++ticks!=100)return;
        try{
            var server=e.getServer();var l=server.overworld();var path=server.getWorldPath(LevelResource.ROOT).resolve("airship-check.txt");
            if(PHASE.equals("seed")){
                var container=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(l);
                for(var old:java.util.List.copyOf(AirshipSystem.ships(l)))container.removeSubLevel(old,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
                var root=new BlockPos(24,170,24);
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)for(int y=-2;y<=2;y++)l.setBlockAndUpdate(root.offset(x,y,z),Blocks.AIR.defaultBlockState());
                l.setBlockAndUpdate(root,AirshipContent.CONTROLLER.get().defaultBlockState());
                l.setBlockAndUpdate(root.below(),Blocks.OAK_PLANKS.defaultBlockState());
                l.setBlockAndUpdate(root.east(),Blocks.CHEST.defaultBlockState());
                ((ChestBlockEntity)l.getBlockEntity(root.east())).setItem(0,KilnContent.STEEL.toStack(23));
                ((AirshipBlockEntity)l.getBlockEntity(root)).power(1e250);
                var p=FakePlayerFactory.getMinecraft(l);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.setPos(root.getX(),root.getY()+1,root.getZ());
                var ship=AirshipSystem.launch(l,root,p,AirshipSystem.scan(l,root,p));
                if(CivicAccess.allowed(l,AirshipSystem.controller(ship),null)||!CivicAccess.allowed(l,AirshipSystem.controller(ship),p))throw new IllegalStateException("Custody failure");
                Files.writeString(path,ship.getUniqueId().toString());System.out.println("AIRSHIP_SEED_PASS");
            }else{
                var id=java.util.UUID.fromString(Files.readString(path).trim());var ship=AirshipSystem.ships(l).stream().filter(s->s.getUniqueId().equals(id)).findFirst().orElseThrow();var root=AirshipSystem.controller(ship);
                if(((AirshipBlockEntity)l.getBlockEntity(root)).power()!=1e250)throw new IllegalStateException("Power changed across restart");
                var stack=((ChestBlockEntity)l.getBlockEntity(root.east())).getItem(0);
                if(stack.getCount()!=23||!stack.is(KilnContent.STEEL.get())||!l.getBlockState(root.below()).is(Blocks.OAK_PLANKS)||!ship.getUserDataTag().hasUUID("owner"))throw new IllegalStateException("Vessel state lost");
                System.out.println("AIRSHIP_RELOAD_PASS uuid="+id+" power=1e250 cargo=23");
            }
            server.halt(false);
        }catch(Exception ex){throw new IllegalStateException("Airship persistence failed",ex);}
    }
}
