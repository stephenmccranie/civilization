package dev.civilization.client;

import dev.civilization.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Client-only guide for one selected nearby controller. Never places blocks or scans chunks. */
@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class MachinePreview {
    private record Ghost(BlockPos pos, String material, boolean wrong) {}
    private static BlockPos selected;
    private static ResourceKey<Level> dimension;
    private static List<Ghost> ghosts = List.of();
    private static boolean works, unloaded;
    private static AABB area;
    private static int missing, wrong;
    private MachinePreview() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { selected = null; ghosts = List.of(); return; }
        if (dimension != mc.level.dimension()) { selected = null; ghosts = List.of(); dimension = mc.level.dimension(); }
        if (mc.screen == null && mc.hitResult instanceof BlockHitResult hit && mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof KilnBlock)
            selected = hit.getBlockPos().immutable();
        if (selected == null) return;
        var state = mc.level.getBlockState(selected);
        if (!(state.getBlock() instanceof KilnBlock) || selected.distToCenterSqr(mc.player.position()) > 32 * 32) {
            selected = null; ghosts = List.of(); return;
        }
        works = state.is(KilnContent.RETORT.get());
        area = new AABB(selected);
        var next = new ArrayList<Ghost>();
        missing = 0; wrong = 0; unloaded = false;
        for (var part : MachineStructure.parts(works)) {
            var pos = MachineStructure.position(selected, state.getValue(AbstractFurnaceBlock.FACING), part);
            area = area.minmax(new AABB(pos));
            if (!mc.level.hasChunkAt(pos)) { unloaded = true; continue; }
            var actual = mc.level.getBlockState(pos);
            if (MachineStructure.matches(actual, part.material())) continue;
            boolean occupied = !actual.isAir();
            if (occupied) wrong++; else missing++;
            next.add(new Ghost(pos, part.material(), occupied));
        }
        ghosts = List.copyOf(next);
    }

    private static boolean visible() {
        var mc = Minecraft.getInstance();
        if (selected == null || area == null || ghosts.isEmpty() || mc.player == null || mc.screen != null || mc.options.hideGui) return false;
        var eye = mc.player.getEyePosition();
        if (area.contains(eye)) return true;
        var hit = area.clip(eye, eye.add(mc.player.getLookAngle().scale(8)));
        if (hit.isEmpty()) return false;
        // A nearer real block outside the build area occludes the guide.
        return mc.hitResult == null || mc.hitResult.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                || mc.hitResult.getLocation().distanceToSqr(eye) + 0.05 >= hit.get().distanceToSqr(eye);
    }

    private static net.minecraft.world.level.block.state.BlockState previewState(Ghost ghost) {
        if (ghost.material().equals("air")) return null;
        var mc = Minecraft.getInstance();
        // Show an accepted variant being held, including stone bricks, aged copper or the input hopper.
        if (mc.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem item) {
            var held = item.getBlock().defaultBlockState();
            if (MachineStructure.matches(held, ghost.material())) return held;
        }
        return switch (ghost.material()) {
            case "stone", "stone_hatch" -> net.minecraft.world.level.block.Blocks.COBBLESTONE.defaultBlockState();
            case "brick", "brick_hatch" -> net.minecraft.world.level.block.Blocks.BRICKS.defaultBlockState();
            case "copper" -> net.minecraft.world.level.block.Blocks.COPPER_BLOCK.defaultBlockState();
            default -> null;
        };
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || !visible()) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        var ghostType = RenderType.entityTranslucent(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
        var translucent = new TranslucentVertexConsumer(buffers.getBuffer(ghostType), 0.30f);
        net.minecraft.client.renderer.MultiBufferSource ghostBuffers = type -> translucent;
        for (var ghost : ghosts) {
            var block = previewState(ghost);
            if (block == null) continue; // An obstructed air chamber gets a red outline, never a fake block.
            pose.pushPose();
            pose.translate(ghost.pos().getX() - 0.002, ghost.pos().getY() - 0.002, ghost.pos().getZ() - 0.002);
            pose.scale(1.004f, 1.004f, 1.004f);
            mc.getBlockRenderer().renderSingleBlock(block, pose, ghostBuffers,
                    net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, ghost.pos()),
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    net.neoforged.neoforge.client.model.data.ModelData.EMPTY, ghostType);
            pose.popPose();
        }
        buffers.endBatch(ghostType);
        for (var ghost : ghosts) {
            var box = new AABB(ghost.pos()).inflate(0.002);
            LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box,
                    1f, ghost.wrong() ? 0.2f : 1f, ghost.wrong() ? 0.2f : 1f, ghost.wrong() ? 0.8f : 0.18f);
        }
        buffers.endBatch(RenderType.lines());
        pose.popPose();
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!visible()) return;
        var graphics = event.getGuiGraphics();
        var label = Component.translatable(works ? "guide.civilization.works" : "guide.civilization.kiln");
        graphics.drawString(mc.font, label, 8, 8, 0xFFFFD180);
        graphics.drawString(mc.font, unloaded ? Component.translatable("gui.civilization.structure_2") :
                ghosts.isEmpty() ? Component.translatable("gui.civilization.structure_1") :
                Component.translatable("preview.civilization.remaining", missing, wrong), 8, 20, 0xFFFFFFFF);
        graphics.drawString(mc.font, Component.translatable("preview.civilization.legend"), 8, 32, 0xFFBBBBBB);
        // Raycast against the guides as well as real blocks to identify an empty target position.
        var start = mc.player.getEyePosition();
        var end = start.add(mc.player.getLookAngle().scale(8));
        Ghost aimed = null;
        double nearest = Double.MAX_VALUE;
        for (var ghost : ghosts) {
            var hit = new AABB(ghost.pos()).clip(start, end);
            if (hit.isPresent() && hit.get().distanceToSqr(start) < nearest) {
                nearest = hit.get().distanceToSqr(start); aimed = ghost;
            }
        }
        if (aimed != null) graphics.drawString(mc.font, Component.translatable("preview.civilization.target",
                Component.translatable("material.civilization." + aimed.material())), 8, 44, aimed.wrong() ? 0xFFFF8888 : 0xFF88DDFF);
    }
}
