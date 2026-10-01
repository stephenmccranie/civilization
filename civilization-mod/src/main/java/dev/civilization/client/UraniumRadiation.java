package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.civilization.FrontierContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Sparse, client-side tracks originating at uranium-bearing object edges. */
@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class UraniumRadiation {
    private record Key(int kind, BlockPos block, UUID player, boolean offhand) {}
    private record Source(Key key, AABB bounds) {}
    private record Track(List<Vec3> points, Vec3 fork, long born, int life) {}

    private static final Random RANDOM = new Random();
    private static final Map<Key, Long> NEXT = new HashMap<>();
    private static final List<Track> ACTIVE = new ArrayList<>();
    private static List<BlockPos> chests = List.of(), ores = List.of();
    private static ClientLevel world;

    private UraniumRadiation() {}

    public static void accept(List<BlockPos> positions) {
        if (Minecraft.getInstance().level != null) chests = List.copyOf(positions);
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (world != mc.level) {
            world = mc.level;
            chests = ores = List.of();
            NEXT.clear();
            ACTIVE.clear();
        }
        if (world == null || mc.player == null || mc.isPaused()) return;
        long now = world.getGameTime();
        if (now % 40 == 0) ores = exposedOre(world, mc.player.blockPosition());
        ACTIVE.removeIf(track -> now - track.born >= track.life);
        var present = new HashSet<Key>();
        for (var pos : ores) {
            if (!world.hasChunkAt(pos) || !world.getBlockState(pos).is(FrontierContent.URANIUM_ORE.get())) continue;
            emit(new Source(new Key(0, pos, null, false), new AABB(pos)), now, present);
        }
        for (var pos : chests) {
            if (!world.hasChunkAt(pos) || !(world.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.ChestBlock)) continue;
            var box = new AABB(pos.getX() + .0625, pos.getY(), pos.getZ() + .0625,
                    pos.getX() + .9375, pos.getY() + .875, pos.getZ() + .9375);
            emit(new Source(new Key(1, pos, null, false), box), now, present);
        }
        for (var player : world.players()) {
            if (player.isRemoved() || player.distanceToSqr(mc.player) > 24 * 24) continue;
            held(player, false, mc.player, now, present);
            held(player, true, mc.player, now, present);
        }
        NEXT.keySet().retainAll(present);
    }

    private static List<BlockPos> exposedOre(ClientLevel level, BlockPos center) {
        var found = new ArrayList<BlockPos>();
        for (var mutable : BlockPos.betweenClosed(center.offset(-10, -8, -10), center.offset(10, 8, 10))) {
            if (mutable.distSqr(center) > 100 || !level.hasChunkAt(mutable)
                    || !level.getBlockState(mutable).is(FrontierContent.URANIUM_ORE.get())) continue;
            boolean exposed = false;
            for (var direction : Direction.values()) {
                var neighbor = mutable.relative(direction);
                if (level.hasChunkAt(neighbor) && !level.getBlockState(neighbor).isSolidRender(level, neighbor)) {
                    exposed = true;
                    break;
                }
            }
            if (exposed) found.add(mutable.immutable());
            if (found.size() == 64) break;
        }
        return List.copyOf(found);
    }

    private static boolean radioactive(ItemStack stack) {
        return stack.is(FrontierContent.RAW_URANIUM.get())
                || stack.is(FrontierContent.URANIUM_ORE.asItem());
    }

    private static void held(Player player, boolean offhand, Player local, long now, HashSet<Key> present) {
        if (!radioactive(offhand ? player.getOffhandItem() : player.getMainHandItem())) return;
        if (player == local && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            var forward = player.getLookAngle();
            var right = new Vec3(-Math.cos(Math.toRadians(player.getYRot())), 0,
                    -Math.sin(Math.toRadians(player.getYRot())));
            var up = right.cross(forward).normalize();
            var center = player.getEyePosition().add(forward.scale(.95))
                    .add(right.scale(offhand ? -.55 : .55)).add(up.scale(-.48));
            var box = new AABB(center.add(-.16, -.16, -.16), center.add(.16, .16, .16));
            emit(new Source(new Key(3, null, player.getUUID(), offhand), box), now, present);
            return;
        }
        var forward = player.getLookAngle();
        var right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 center;
        if (player == local) {
            center = player.getEyePosition().add(forward.scale(.65))
                    .add(right.scale(offhand ? -.48 : .48)).add(0, -.16, 0);
        } else {
            center = player.position().add(0, 1.25, 0).add(right.scale(offhand ? -.3 : .3));
        }
        var box = new AABB(center.add(-.16, -.16, -.16), center.add(.16, .16, .16));
        emit(new Source(new Key(2, null, player.getUUID(), offhand), box), now, present);
    }

    private static void emit(Source source, long now, HashSet<Key> present) {
        present.add(source.key);
        long due = NEXT.computeIfAbsent(source.key, key -> now + delay());
        if (now < due || ACTIVE.size() >= 128) return;
        ACTIVE.add(track(source.key, source.bounds, now));
        NEXT.put(source.key, now + delay());
    }

    private static int delay() {
        // Exponential waiting gives natural quiet seconds and occasional close pairs.
        return Math.max(2, Math.min(70, (int) Math.ceil(-Math.log(1 - RANDOM.nextDouble()) * 15.3)));
    }

    private static Track track(Key source, AABB box, long now) {
        int axis = RANDOM.nextInt(3);
        double[] low = {box.minX, box.minY, box.minZ};
        double[] high = {box.maxX, box.maxY, box.maxZ};
        double[] point = new double[3], normal = new double[3];
        for (int i = 0; i < 3; i++) {
            if (i == axis) point[i] = low[i] + RANDOM.nextDouble() * (high[i] - low[i]);
            else {
                normal[i] = RANDOM.nextBoolean() ? 1 : -1;
                point[i] = normal[i] > 0 ? high[i] : low[i];
            }
        }
        var outward = new Vec3(normal[0], normal[1], normal[2]).normalize();
        var start = new Vec3(point[0], point[1], point[2]).add(outward.scale(.015));
        var direction = outward.add((RANDOM.nextDouble() - .5) * .55,
                (RANDOM.nextDouble() - .5) * .55, (RANDOM.nextDouble() - .5) * .55).normalize();
        double length = 2.5 + RANDOM.nextDouble();
        var points = new ArrayList<Vec3>();
        for (int i = 0; i <= 5; i++) {
            double fraction = i / 5.0;
            var position = start.add(direction.scale(length * fraction));
            if (i > 0 && i < 5) position = position.add((RANDOM.nextDouble() - .5) * .08,
                    (RANDOM.nextDouble() - .5) * .08, (RANDOM.nextDouble() - .5) * .08);
            points.add(position);
        }
        Vec3 fork = null;
        if (RANDOM.nextDouble() < .28) fork = points.get(2).add(direction.add(
                (RANDOM.nextDouble() - .5) * 1.5, (RANDOM.nextDouble() - .5) * 1.5,
                (RANDOM.nextDouble() - .5) * 1.5).normalize().scale(length * .25));
        return new Track(List.copyOf(points), fork, now, 14 + RANDOM.nextInt(13));
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || ACTIVE.isEmpty()) return;
        var mc = Minecraft.getInstance();
        if (mc.level != world || mc.player == null) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var matrix = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        var previousColor = com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
        matrix.pushMatrix();
        matrix.mul(event.getModelViewMatrix());
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false);
        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            var buffers = mc.renderBuffers().bufferSource();
            var lines = buffers.getBuffer(PreviewRenderTypes.OUTLINE);
            long now = world.getGameTime();
            for (var track : ACTIVE) {
                draw(pose, lines, track, now);
            }
            buffers.endBatch(PreviewRenderTypes.OUTLINE);
        } finally {
            pose.popPose();
            matrix.popMatrix();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
        }
    }

    private static void draw(PoseStack pose, VertexConsumer lines, Track track, long now) {
        int age = (int) (now - track.born);
        if (age < 0 || age >= track.life) return;
        int last = age == 0 ? 3 : 5;
        float fade = age < 3 ? 1 : Math.max(0, (track.life - age) / (float) (track.life - 3));
        float alpha = .72f * fade;
        for (int i = 0; i < last; i++) line(pose, lines,
                track.points.get(i), track.points.get(i + 1),
                alpha * tipFade(i, last), alpha * tipFade(i + 1, last));
        if (track.fork != null && age >= 2)
            line(pose, lines, track.points.get(2), track.fork, alpha * .4f, 0);
    }

    private static float tipFade(int point, int last) {
        return (float) Math.sin(Math.PI * point / last);
    }

    private static void line(PoseStack pose, VertexConsumer lines, Vec3 from, Vec3 to,
                             float startAlpha, float endAlpha) {
        var normal = to.subtract(from).normalize();
        var entry = pose.last();
        lines.addVertex(entry, (float) from.x, (float) from.y, (float) from.z)
                .setColor(.84f, .85f, .84f, startAlpha)
                .setNormal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
        lines.addVertex(entry, (float) to.x, (float) to.y, (float) to.z)
                .setColor(.84f, .85f, .84f, endAlpha)
                .setNormal(entry, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
