package dev.civilization.client;

import dev.civilization.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
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
    private record Ghost(BlockPos pos, MachineStructure.Part part, net.minecraft.core.Direction front, boolean wrong, AABB box) {
        String material() { return part.material(); }
        String label() { return material() + (part.units() == 4 ? "" : part.units() == 2 ? "_half" : part.units()==3?"_eighth":"_quarter"); }
    }
    private record Need(String material, int units) {}
    private static BlockPos selected, focusedController;
    private static final GuideFocus focus=new GuideFocus();
    private static boolean showGuide;
    private static ResourceKey<Level> dimension;
    private static List<Ghost> ghosts = List.of();
    private static List<AABB> blockers = List.of();
    private static java.util.Map<Need,Integer> needs = java.util.Map.of();
    private static boolean works, unloaded, survey;
    private static IndustrialBlock.Kind industry;
    private static AABB area;
    private static int missing, wrong;
    private MachinePreview() {}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { selected = null; ghosts = List.of(); blockers = List.of(); focus.reset();showGuide=false;return; }
        if (dimension != mc.level.dimension()) { selected = null; ghosts = List.of(); blockers = List.of(); focus.reset();showGuide=false;dimension = mc.level.dimension(); }
        if (mc.screen == null && mc.hitResult instanceof BlockHitResult hit && (mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof BulkBlock || mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof OilEngineBlock || mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof IndustrialBlock || mc.level.getBlockState(hit.getBlockPos()).getBlock() instanceof KilnBlock || mc.level.getBlockState(hit.getBlockPos()).is(CivicContent.TABLE.get())))
            selected = hit.getBlockPos().immutable();
        if (selected == null) return;
        var state = mc.level.getBlockState(selected);
        if (!(state.getBlock() instanceof BulkBlock) && !(state.getBlock() instanceof OilEngineBlock) && !(state.getBlock() instanceof IndustrialBlock) && !(state.getBlock() instanceof KilnBlock) && !state.is(CivicContent.TABLE.get()) || selected.distToCenterSqr(mc.player.position()) > 32 * 32) {
            selected = null; ghosts = List.of(); blockers = List.of(); focus.reset();showGuide=false;return;
        }
        works = state.is(KilnContent.RETORT.get()); survey=state.is(CivicContent.TABLE.get());
        industry=state.getBlock() instanceof IndustrialBlock b?b.kind:null;
        area = new AABB(selected);
        var next = new ArrayList<Ghost>();
        missing = 0; wrong = 0; unloaded = false;
        for (var part : MachineStructure.guideParts(state)) {
            var pos = MachineStructure.position(selected, state.getValue(AbstractFurnaceBlock.FACING), part);
            area = area.minmax(new AABB(pos));
            if (!mc.level.hasChunkAt(pos)) { unloaded = true; continue; }
            var actual = mc.level.getBlockState(pos);
            if (MachineConstruction.present(mc.level, pos, part, state.getValue(AbstractFurnaceBlock.FACING))) continue;
            boolean occupied = MachineConstruction.blocked(mc.level,pos,part,state.getValue(AbstractFurnaceBlock.FACING));
            if (occupied) wrong++; else missing++;
            var front = state.getValue(AbstractFurnaceBlock.FACING);
            next.add(new Ghost(pos, part, front, occupied, bounds(pos, part, front)));
        }
        ghosts = List.copyOf(next);
        blockers = ghosts.stream().map(ghost -> ghost.box().deflate(.0001)).distinct().toList();
        var counts = new java.util.LinkedHashMap<Need,Integer>();
        for (var ghost : ghosts) if (!ghost.material().equals("air")) {
            String material = ghost.material().endsWith("_port") ? "port" : ghost.material();
            counts.merge(new Need(material, ghost.part().units()), 1, Integer::sum);
        }
        needs = java.util.Collections.unmodifiableMap(counts);
    }

    private static boolean eligible() {
        var mc=Minecraft.getInstance();
        return selected!=null && area!=null && !ghosts.isEmpty() && mc.player!=null && mc.level!=null && mc.screen==null && !mc.options.hideGui;
    }
    private static boolean visible() {
        var mc=Minecraft.getInstance();
        if(!eligible()){focus.reset();return false;}
        if(!selected.equals(focusedController)){focusedController=selected;focus.reset();}
        // Use one interpolated camera ray for both aiming and real-world obstruction.
        // Mixing tick-position eyes with the frame's vanilla pick ray caused edge flicker while walking.
        var camera=mc.gameRenderer.getMainCamera();var eye=camera.getPosition();
        var forward=camera.getLookVector();
        var end=eye.add(forward.x()*8,forward.y()*8,forward.z()*8);
        var target=area.inflate(focus.visible()?.1:.015);
        var hit=target.clip(eye,end);
        boolean aimed=target.contains(eye)||hit.isPresent();
        if(aimed&&!target.contains(eye)){
            var real=mc.level.clip(new net.minecraft.world.level.ClipContext(eye,end,
                    net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
            aimed=real.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK
                    ||area.intersects(new AABB(real.getBlockPos()))
                    ||real.getLocation().distanceTo(eye)+.03>=hit.orElseThrow().distanceTo(eye);
        }
        return focus.update(aimed,net.minecraft.Util.getMillis());
    }

    private static net.minecraft.world.level.block.state.BlockState previewState(Ghost ghost) {
        var mc = Minecraft.getInstance();
        // Show an accepted variant being held, including stone bricks, aged copper or the input hopper.
        var held = mc.player.getMainHandItem().getItem() instanceof net.minecraft.world.item.BlockItem item
                ? item.getBlock().defaultBlockState() : null;
        return MachineStructure.previewState(ghost.part(), ghost.front(), held);
    }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        // Iris finishes its composite pass when LevelRenderer returns. AFTER_LEVEL runs after
        // that, before vanilla clears world depth for the hand/HUD. No pack-specific shaders.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        showGuide=visible();if(!showGuide)return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        var pose = event.getPoseStack();
        var camera = event.getCamera().getPosition();
        var buffers = mc.renderBuffers().bufferSource();
        var shaderColor=com.mojang.blaze3d.systems.RenderSystem.getShaderColor().clone();
        // The guide is an independent overlay, not part of the preceding world/placement tint.
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        boolean textured = PreviewConfig.MODE.get() == PreviewConfig.Mode.TEXTURED;
        var renderMatrix = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
        renderMatrix.pushMatrix();
        renderMatrix.mul(event.getModelViewMatrix());
        com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
        mc.getMainRenderTarget().bindWrite(false);
        pose.pushPose();
        try {
            pose.translate(-camera.x, -camera.y, -camera.z);
            var visibleGhosts = new ArrayList<Ghost>();
            var frustum = event.getFrustum();
            for (var ghost : ghosts) if (frustum == null || frustum.isVisible(ghost.box().inflate(.25)))
                visibleGhosts.add(ghost);
            var ghostType = PreviewRenderTypes.TEXTURED;
            if (textured) {
                var translucent = new TranslucentVertexConsumer(buffers.getBuffer(ghostType), PreviewConfig.OPACITY.get().floatValue());
                net.minecraft.client.renderer.MultiBufferSource ghostBuffers = type -> translucent;
                for (var ghost : visibleGhosts) {
                    // Occupied cells already have a real surface: use the red outline only.
                    // Layering a full-bright ghost onto newly rebuilt terrain causes a bright flash.
                    if(MachineStructure.obstructed(mc.level,ghost.pos(),ghost.part(),ghost.front()))continue;
                    var block = previewState(ghost);
                    if (block == null) continue; // An obstructed air chamber gets a red outline, never a fake block.
                    pose.pushPose();
                    pose.translate(ghost.pos().getX() - 0.002, ghost.pos().getY() - 0.002, ghost.pos().getZ() - 0.002);
                    pose.scale(1.004f, 1.004f, 1.004f);
                    mc.getBlockRenderer().renderSingleBlock(block, pose, ghostBuffers,
                            net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                            net.neoforged.neoforge.client.model.data.ModelData.builder().with(CutBlockEntity.MATERIAL, previewMaterial(ghost)).build(), ghostType);
                    pose.popPose();
                }
                buffers.endBatch(ghostType);
            }
            var aimed=aimedGhost();
            boolean largeGuide = ghosts.size() > 128;
            int outlineSteps = largeGuide ? 6 : 16;
            // Large open frames remain legible through all their translucent parts, but
            // only the nearest edges need hard outlines. This also bounds ray tests.
            List<Ghost> outlined = visibleGhosts;
            if (largeGuide && visibleGhosts.size() > 64) {
                var nearest = new ArrayList<>(visibleGhosts);
                nearest.sort(java.util.Comparator.comparingDouble(ghost -> camera.distanceToSqr(ghost.box().getCenter())));
                nearest.subList(64, nearest.size()).clear();
                if (aimed != null && visibleGhosts.contains(aimed) && !nearest.contains(aimed)) nearest.add(aimed);
                outlined = nearest;
            }
            for (var ghost : outlined) {
                boolean highlight=ghost==aimed;
                GuideOutline.draw(pose, buffers.getBuffer(PreviewRenderTypes.OUTLINE), ghost.box().inflate(.002),blockers,camera,outlineSteps,
                        ghost.wrong()?1f:highlight?.35f:1f, ghost.wrong() ? 0.2f : 1f, ghost.wrong() ? 0.2f : 1f,
                        ghost.wrong() ? 0.8f : highlight?.75f:textured ? 0.22f : 0.7f);
            }
            buffers.endBatch(PreviewRenderTypes.OUTLINE);
        } finally {
            pose.popPose();
            renderMatrix.popMatrix();
            com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(shaderColor[0],shaderColor[1],shaderColor[2],shaderColor[3]);
        }
    }

    @SubscribeEvent public static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (!showGuide || !eligible()) return;
        var graphics = event.getGuiGraphics();
        var label = mc.level.getBlockState(selected).getBlock() instanceof BulkBlock||industry!=null||mc.level.getBlockState(selected).getBlock() instanceof OilEngineBlock||mc.level.getBlockState(selected).getBlock() instanceof KilnBlock?mc.level.getBlockState(selected).getBlock().getName():Component.translatable(survey ? "guide.civilization.survey" : works ? "guide.civilization.works" : "guide.civilization.kiln");
        graphics.drawString(mc.font, label, 8, 8, 0xFFFFD180);
        graphics.drawString(mc.font, unloaded ? Component.translatable("gui.civilization.structure_2") :
                ghosts.isEmpty() ? Component.translatable("gui.civilization.structure_1") :
                Component.translatable("preview.civilization.remaining", missing, wrong), 8, 20, 0xFFFFFFFF);
        graphics.drawString(mc.font, Component.translatable(PreviewConfig.MODE.get() == PreviewConfig.Mode.TEXTURED
                ? "preview.civilization.legend" : "preview.civilization.legend_outline"), 8, 32, 0xFFBBBBBB);
        if (!needs.isEmpty()) graphics.drawString(mc.font, Component.translatable("preview.civilization.materials"), 8, 44, 0xFFFFD180);
        int index = 0, rows = Math.min(8, needs.size());
        for (var entry : needs.entrySet()) {
            int x = 8 + (index / 8) * 180, y = 56 + (index % 8) * 10;
            var name = Component.translatable("preview.civilization.material." + entry.getKey().material());
            var line = Component.literal(entry.getValue() + "× ").append(name);
            if (entry.getKey().units() != 4) line.append(Component.literal(" ")).append(Component.translatable("preview.civilization.size." + entry.getKey().units()));
            graphics.drawString(mc.font, line, x, y, 0xFFFFFFFF);
            index++;
        }
        var aimed=aimedGhost();
        if (aimed != null) graphics.drawString(mc.font, Component.translatable("preview.civilization.target",
                Component.translatable("material.civilization." + aimed.label())), 8, 56 + rows * 10 + 4, aimed.wrong() ? 0xFFFF8888 : 0xFF88DDFF);
    }
    private static Ghost aimedGhost() {
        var mc=Minecraft.getInstance();
        var camera=mc.gameRenderer.getMainCamera();
        var start = camera.getPosition();var forward=camera.getLookVector();
        var end = start.add(forward.x()*8,forward.y()*8,forward.z()*8);
        Ghost aimed = null;
        double nearest = mc.hitResult!=null && mc.hitResult.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK
                ?mc.hitResult.getLocation().distanceToSqr(start)+.01:64;
        for (var ghost : ghosts) {
            var hit = ghost.box().clip(start, end);
            if (hit.isPresent() && hit.get().distanceToSqr(start) < nearest) {
                nearest = hit.get().distanceToSqr(start); aimed = ghost;
            }
        }
        return aimed;
    }
    private static net.minecraft.world.level.block.state.BlockState previewMaterial(Ghost ghost) {
        var held = Minecraft.getInstance().player.getMainHandItem();
        var material = CuttingContent.material(held);
        return MachineStructure.matches(material, ghost.material()) ? material : MachineStructure.materialState(ghost.material());
    }
    private static AABB bounds(BlockPos pos, MachineStructure.Part part, net.minecraft.core.Direction front) {
        var block = MachineStructure.previewState(part, front, null);
        return block != null && block.is(CuttingContent.PIECE.get())
                ? CutBlock.bounds(block).move(pos) : new AABB(pos);
    }
}
