package dev.civilization.lab;

import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.nio.file.*;
import java.util.*;

@EventBusSubscriber(modid="civilization")
public final class SableLab {
    private static final String PHASE=System.getProperty("civilization.sablePhase","manual");
    private static int ticks;
    private static ServerSubLevel fixture;
    private static org.joml.Vector3d initial;
    private static boolean failed;
    @SubscribeEvent public static void physics(ForgeSablePrePhysicsTickEvent e){for(var ship:SableAdapter.platforms(e.getPhysicsSystem().getLevel()))SableAdapter.step(ship,e.getTimeStep());}
    @SubscribeEvent public static void start(ServerStartedEvent e){ticks=0;fixture=null;initial=null;failed=false;SableAdapter.clear();if(!PHASE.equals("manual"))for(int x=-2;x<=5;x++)for(int z=-2;z<=2;z++)e.getServer().overworld().setChunkForced(x,z,true);}
    @SubscribeEvent public static void stop(ServerStoppedEvent e){SableAdapter.clear();}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        var root=Commands.literal("civphysics").requires(s->s.hasPermission(2));
        root.then(Commands.literal("spawn").executes(c->{var p=c.getSource().getPlayerOrException();var ship=SableAdapter.create(p.serverLevel(),p.blockPosition().offset(6,3,0));c.getSource().sendSuccess(()->Component.literal("Created lab platform "+ship.getUniqueId()),false);return 1;}));
        root.then(Commands.literal("drive").then(Commands.argument("throttle",DoubleArgumentType.doubleArg(-1,1)).then(Commands.argument("steer",DoubleArgumentType.doubleArg(-1,1)).executes(c->{var ship=nearest(c.getSource());SableAdapter.control(ship,DoubleArgumentType.getDouble(c,"throttle"),DoubleArgumentType.getDouble(c,"steer"),false);return 1;}))));
        root.then(Commands.literal("brake").executes(c->{SableAdapter.control(nearest(c.getSource()),0,0,true);return 1;}));
        root.then(Commands.literal("board").executes(c->{var ship=nearest(c.getSource());var p=c.getSource().getPlayerOrException();var at=SableAdapter.deck(ship);p.teleportTo(p.serverLevel(),at.x,at.y,at.z,Set.of(),p.getYRot(),0);return 1;}));
        root.then(Commands.literal("status").executes(c->{var ship=nearest(c.getSource());c.getSource().sendSuccess(()->Component.literal("Lab "+ship.getUniqueId()+" | speed "+String.format(Locale.ROOT,"%.2f",SableAdapter.speed(ship))+" m/s | mass "+ship.getMassTracker().getMass()),false);return 1;}));
        e.getDispatcher().register(root);
    }
    private static ServerSubLevel nearest(net.minecraft.commands.CommandSourceStack source)throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return SableAdapter.platforms(source.getLevel()).stream().min(Comparator.comparingDouble(s->s.logicalPose().position().distanceSquared(source.getPosition().x,source.getPosition().y,source.getPosition().z))).orElseThrow(()->new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("No loaded lab platform")).create());
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        if(PHASE.equals("manual")||failed)return;
        ticks++;var server=e.getServer();var level=server.overworld();
        try{
            if(ticks==80){
                var ships=SableAdapter.platforms(level);
                if(PHASE.equals("seed")){
                    fixture=ships.isEmpty()?SableAdapter.create(level,new BlockPos(14,230,0)):ships.getFirst();
                    SableAdapter.assertCargo(fixture);initial=new org.joml.Vector3d(fixture.logicalPose().position());SableAdapter.control(fixture,1,0,false);
                }else {
                    String id=Files.readString(server.getWorldPath(LevelResource.ROOT).resolve("civilization-lab-checkpoint.txt")).trim();
                    fixture=ships.stream().filter(s->s.getUniqueId().toString().equals(id)).findFirst().orElseThrow(()->new IllegalStateException("Saved vessel UUID not restored"));
                    SableAdapter.assertCargo(fixture);System.out.println("CIV_SABLE_RELOAD_PASS uuid="+id+" cargo=23 mixed_cells=3");server.halt(false);
                }
            }
            if(PHASE.equals("seed")&&fixture!=null){
                if(ticks==180){double moved=fixture.logicalPose().position().distance(initial);if(moved<8)throw new IllegalStateException("Propulsion did not move platform: "+moved);System.out.println("CIV_SABLE_TRANSLATION_PASS distance="+moved);SableAdapter.control(fixture,.5,.7,false);}
                if(ticks==220){if(Math.abs(fixture.logicalPose().orientation().y())<.02)throw new IllegalStateException("Steering did not rotate platform");SableAdapter.control(fixture,0,0,true);}
                if(ticks==280){SableAdapter.assertCargo(fixture);if(SableAdapter.speed(fixture)>.5)throw new IllegalStateException("Brake did not stop platform: "+SableAdapter.speed(fixture));Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("civilization-lab-checkpoint.txt"),fixture.getUniqueId().toString());System.out.println("CIV_SABLE_SEED_PASS uuid="+fixture.getUniqueId()+" cargo=23 steering=true braking=true");server.halt(false);}
            }
            if(ticks>600)throw new IllegalStateException("Lab timed out");
        }catch(Exception failure){failed=true;throw new IllegalStateException("Sable laboratory failed in "+PHASE,failure);}
    }
}
