package dev.civilization.client;

import dev.civilization.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.model.data.ModelData;

/** Client-only guide for one structure: where the held block would be placed, or one selected nearby controller.
 * Never places blocks or scans chunks. */
@EventBusSubscriber(modid = "civilization", value = Dist.CLIENT)
public final class MachinePreview {
    /** A structure requirement, or, without a part, a cell the held block's click fills. Its look is prepared outside rendering. */
    private record Ghost(BlockPos pos, AABB box, MachineStructure.Part part, BlockState shown, ModelData data, boolean wrong) {
        String label() { return part.material() + (part.units() == 4 ? "" : part.units() == 2 ? "_half" : "_quarter"); }
    }
    private record LayoutKey(Level level,BlockPos at,BlockState state) {}
    private record LayoutPart(BlockPos pos,MachineStructure.Part part,net.minecraft.core.Direction front,AABB box) {}
    private record UpdateKey(LayoutKey layout,PlacementPlan plan,net.minecraft.world.item.Item held,int count,BlockState material,int units) {}
    private static LayoutKey layoutKey;
    private static List<LayoutPart> layout=List.of();
    private static final GuidePerformance.Refresh refresh=new GuidePerformance.Refresh();
    private record Need(String material, int units) {}
    private static BlockPos selected, focusedController;
    private static PlacementPlan plan;
    private static final GuideFocus focus=new GuideFocus();
    private static boolean showGuide;
    private static ResourceKey<Level> dimension;
    private static List<Ghost> ghosts = List.of();
    private static List<AABB> blockers = List.of();
    private static java.util.Map<Need,Integer> needs = java.util.Map.of();
    private static boolean unloaded;
    private static AABB area;
    private static int missing, wrong;
    private static final float[] WRONG={1,.2f,.2f,.8f}, PLACED={.5f,1,1,.65f}, AIMED={.35f,1,1,.75f}, FAINT={1,1,1,.22f}, PLAIN={1,1,1,.7f};
    private MachinePreview() {}

    private static boolean guided(BlockState state) { return !MachineStructure.guideParts(state).isEmpty(); }
    private static void clear() { selected = null; plan = null; ghosts = List.of(); blockers = List.of(); focus.reset();refresh.reset();layoutKey=null;layout=List.of();showGuide=false; }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) { clear(); return; }
        if (dimension != mc.level.dimension()) { clear(); dimension = mc.level.dimension(); }
        // A held block that fills several cells or brings a structure shows where a right-click would put it.
        plan = PlacementPlan.of(mc);
        if (plan != null && plan.cells().size() == 1 && !guided(plan.state())) plan = null;
        if (mc.screen == null && mc.hitResult instanceof BlockHitResult hit && guided(mc.level.getBlockState(hit.getBlockPos())))
            selected = hit.getBlockPos().immutable();
        if (selected != null && (!guided(mc.level.getBlockState(selected)) || selected.distToCenterSqr(mc.player.position()) > 32 * 32)) selected = null;
        if (plan == null && selected == null) { clear(); return; }
        var at = plan != null ? plan.at() : selected;
        var state = plan != null ? plan.state() : mc.level.getBlockState(at);
        var key=new LayoutKey(mc.level,at,state);
        if(!key.equals(layoutKey)){
            layoutKey=key;var parts=new ArrayList<LayoutPart>();area=new AABB(at);
            var front=state.getValue(AbstractFurnaceBlock.FACING);
            for(var part:MachineStructure.guideParts(state)){var pos=MachineStructure.position(at,front,part);area=area.minmax(new AABB(pos));parts.add(new LayoutPart(pos,part,front,bounds(pos,part,front)));}
            layout=List.copyOf(parts);
        }
        var held=mc.player.getMainHandItem();
        if(!refresh.due(new UpdateKey(key,plan,held.getItem(),held.getCount(),CuttingContent.material(held),CuttingContent.units(held))))return;
        // Show an accepted variant being held, including stone bricks, aged copper or the input hopper.
        var heldBlock = held.getItem() instanceof net.minecraft.world.item.BlockItem item ? item.getBlock().defaultBlockState() : null;
        var heldMaterial = CuttingContent.material(held);
        var next = new ArrayList<Ghost>();missing=0;wrong=0;unloaded=false;
        // A refused placement is outlined in red only, never drawn as if it would be built.
        if (plan != null) for (var cell : plan.cells())
            next.add(new Ghost(cell.pos(), new AABB(cell.pos()), null, plan.valid() ? cell.state() : null, ModelData.EMPTY, !plan.valid()));
        for(var entry:layout){
            var pos=entry.pos();var part=entry.part();var front=entry.front();
            if(!mc.level.hasChunkAt(pos)){unloaded=true;continue;}
            if(MachineStructure.matches(mc.level,pos,part,front))continue;
            boolean occupied=!mc.level.getBlockState(pos).isAir();
            if(occupied)wrong++;else missing++;
            // Occupied cells already have a real surface: use the red outline only. Layering a
            // full-bright ghost onto newly rebuilt terrain causes a bright flash. An obstructed
            // air chamber likewise gets a red outline, never a fake block.
            var shown = occupied ? null : MachineStructure.previewState(part, front, heldBlock);
            var material = MachineStructure.matches(heldMaterial, part.material()) ? heldMaterial : MachineStructure.materialState(part.material());
            next.add(new Ghost(pos,entry.box(),part,shown,ModelData.builder().with(CutBlockEntity.MATERIAL, material).build(),occupied));
        }
        ghosts = List.copyOf(next);
        blockers = ghosts.stream().map(ghost -> ghost.box().deflate(.0001)).distinct().toList();
        var counts = new java.util.LinkedHashMap<Need,Integer>();
        for (var ghost : ghosts) if (ghost.part() != null && !ghost.part().material().equals("air"))
            counts.merge(new Need(ghost.part().material(), ghost.part().units()), 1, Integer::sum);
        needs = java.util.Collections.unmodifiableMap(counts);
    }

    private static boolean eligible() {
        var mc=Minecraft.getInstance();
        return (plan!=null || selected!=null) && area!=null && !ghosts.isEmpty() && mc.player!=null && mc.level!=null && mc.screen==null && !mc.options.hideGui;
    }
    private static boolean visible() {
        var mc=Minecraft.getInstance();
        if(!eligible()){focus.reset();return false;}
        if(plan!=null)return true; // A held placement shows wherever it is aimed.
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

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        // Iris finishes its composite pass when LevelRenderer returns. AFTER_LEVEL runs after
        // that, before vanilla clears world depth for the hand/HUD. No pack-specific shaders.
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        showGuide=visible();if(!showGuide)return;
        var mc = Minecraft.getInstance();
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
                    if (ghost.shown() == null) continue;
                    pose.pushPose();
                    pose.translate(ghost.pos().getX() - 0.002, ghost.pos().getY() - 0.002, ghost.pos().getZ() - 0.002);
                    pose.scale(1.004f, 1.004f, 1.004f);
                    mc.getBlockRenderer().renderSingleBlock(ghost.shown(), pose, ghostBuffers,
                            net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, ghost.data(), ghostType);
                    pose.popPose();
                }
                buffers.endBatch(ghostType);
            }
            var aimed=aimedGhost();
            int outlineSteps=GuidePerformance.outlineSteps(ghosts.size());
            var outlined=GuidePerformance.outlines(visibleGhosts,ghost->true,ghost->camera.distanceToSqr(ghost.box().getCenter()),aimed);
            for (var ghost : outlined) {
                var color = ghost.wrong() ? WRONG : ghost.part() == null ? PLACED : ghost == aimed ? AIMED : textured ? FAINT : PLAIN;
                GuideOutline.draw(pose, buffers.getBuffer(PreviewRenderTypes.OUTLINE), ghost.box().inflate(.002),blockers,camera,outlineSteps,
                        color[0], color[1], color[2], color[3]);
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
        var state = layoutKey.state();
        graphics.drawString(mc.font, state.is(CivicContent.TABLE.get()) ? Component.translatable("guide.civilization.survey") : state.getBlock().getName(), 8, 8, 0xFFFFD180);
        int top = 20;
        if (plan != null) { graphics.drawString(mc.font, Component.translatable(plan.valid() ? "preview.civilization.place" : "preview.civilization.blocked"),
                8, top, plan.valid() ? 0xFFFFFFFF : 0xFFFF8888); top += 12; }
        if (!layout.isEmpty()) {
            graphics.drawString(mc.font, unloaded ? Component.translatable("gui.civilization.structure_2") :
                    Component.translatable("preview.civilization.remaining", missing, wrong), 8, top, 0xFFFFFFFF);
            graphics.drawString(mc.font, Component.translatable(PreviewConfig.MODE.get() == PreviewConfig.Mode.TEXTURED
                    ? "preview.civilization.legend" : "preview.civilization.legend_outline"), 8, top + 12, 0xFFBBBBBB);
            top += 24;
        }
        if (!needs.isEmpty()) graphics.drawString(mc.font, Component.translatable("preview.civilization.materials"), 8, top, 0xFFFFD180);
        int index = 0, rows = Math.min(8, needs.size()), list = top + 12;
        for (var entry : needs.entrySet()) {
            int x = 8 + (index / 8) * 180, y = list + (index % 8) * 10;
            var name = Component.translatable("preview.civilization.material." + entry.getKey().material());
            var line = Component.literal(entry.getValue() + "× ").append(name);
            if (entry.getKey().units() != 4) line.append(Component.literal(" ")).append(Component.translatable("preview.civilization.size." + entry.getKey().units()));
            graphics.drawString(mc.font, line, x, y, 0xFFFFFFFF);
            index++;
        }
        var aimed=aimedGhost();
        if (aimed != null) graphics.drawString(mc.font, Component.translatable("preview.civilization.target",
                Component.translatable("material.civilization." + aimed.label())), 8, list + rows * 10 + 4, aimed.wrong() ? 0xFFFF8888 : 0xFF88DDFF);
    }
    /** The structure requirement under the crosshair, in front of any real block. */
    private static Ghost aimedGhost() {
        var mc=Minecraft.getInstance();
        var camera=mc.gameRenderer.getMainCamera();
        var start = camera.getPosition();var forward=camera.getLookVector();
        var end = start.add(forward.x()*8,forward.y()*8,forward.z()*8);
        Ghost aimed = null;
        double nearest = mc.hitResult!=null && mc.hitResult.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK
                ?mc.hitResult.getLocation().distanceToSqr(start)+.01:64;
        for (var ghost : ghosts) {
            if (ghost.part() == null) continue;
            var hit = ghost.box().clip(start, end);
            if (hit.isPresent() && hit.get().distanceToSqr(start) < nearest) {
                nearest = hit.get().distanceToSqr(start); aimed = ghost;
            }
        }
        return aimed;
    }
    private static AABB bounds(BlockPos pos, MachineStructure.Part part, net.minecraft.core.Direction front) {
        var block = MachineStructure.previewState(part, front, null);
        return block != null && block.is(CuttingContent.PIECE.get())
                ? CutBlock.bounds(block).move(pos) : new AABB(pos);
    }
}
