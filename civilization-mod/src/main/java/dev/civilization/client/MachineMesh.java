package dev.civilization.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.civilization.MachineModel;
import dev.civilization.ModeledMachine;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.loading.FileLoader;
import software.bernie.geckolib.loading.object.BakedModelFactory;
import software.bernie.geckolib.loading.object.GeometryTree;
import software.bernie.geckolib.util.RenderUtil;

/**
 * A machine model's still geometry as block quads, worked out once per resource load with GeckoLib's own transforms, so
 * the cells draw exactly what {@link ModeledMachineRenderer} would. For each facing and cell it holds the quads of each
 * section, of the controller and of its lit and cold bones, placed relative to that cell's block with texture
 * coordinates in the model's own texture. A cube is drawn from the cell nearest its centre among those its section
 * fills, the controller's from the controller's, so it takes that cell's light. A bone named `…water` is water, drawn
 * in vanilla's water texture. Bones the model animates or the renderer drives are left out: the block entity renderer
 * draws them.
 */
final class MachineMesh {
    /** Groups besides the sections, numbered 0 up: the controller's bones and the lit and cold bones that follow its lit
     * state; a heated machine's lit bones are its fire and glow. */
    static final int CONTROLLER = -1, LIT = -2, COLD = -3;
    /** The controller's cell, which is not one of the model's part cells. */
    static final int CONTROLLER_CELL = -1;
    /** Four corners' positions and texture coordinates, the normal, whether the quad glows and whether it is water. */
    record Quad(Vector3f[] corners, float[] uv, Vector3f normal, boolean glow, boolean water) {}

    final MachineModel model;
    /** Names of the bones the model's animations move or the renderer drives, and of every bone below them. */
    final Set<String> animated;
    /** Per facing, cell and group, keyed by {@link #key}. */
    private final Map<Long, List<Quad>> quads = new HashMap<>();

    private static final Map<MachineModel, MachineMesh> LOADED = new java.util.concurrent.ConcurrentHashMap<>();

    private MachineMesh(MachineModel model, Set<String> animated) { this.model = model; this.animated = animated; }

    /** The mesh of the last resource load. */
    static MachineMesh of(MachineModel model) { return LOADED.get(model); }

    static long key(Direction front, int cell, int group) { return ((long) front.get2DDataValue() << 40) | ((long) (cell + 1) << 20) | (group + 8); }
    List<Quad> quads(Direction front, int cell, int group) { return quads.getOrDefault(key(front, cell, group), List.of()); }

    static MachineMesh load(MachineModel model, ResourceManager resources) {
        var geo = ResourceLocation.fromNamespaceAndPath("civilization", "geo/" + model.geo + ".geo.json");
        var baked = BakedModelFactory.getForNamespace("civilization").constructGeoModel(GeometryTree.fromModel(FileLoader.loadModelFile(geo, resources)));
        var moved = new HashSet<String>();
        if (model.animation != null) {
            var file = FileLoader.loadFile(ResourceLocation.fromNamespaceAndPath("civilization", "animations/" + model.geo + ".animation.json"), resources);
            for (var animation : file.getAsJsonObject("animations").entrySet())
                if (animation.getValue().getAsJsonObject().has("bones")) moved.addAll(animation.getValue().getAsJsonObject().getAsJsonObject("bones").keySet());
        }
        var mesh = new MachineMesh(model, new HashSet<>());
        for (var bone : baked.topLevelBones()) mesh.animate(bone, moved, false);
        for (var front : Direction.Plane.HORIZONTAL) {
            // GeckoLib's block renderer draws from the controller block's centre, turned to its facing.
            var pose = new PoseStack(); pose.translate(.5, 0, .5);
            pose.mulPose(Axis.YP.rotationDegrees(switch (front) { case SOUTH -> 180; case WEST -> 90; case EAST -> 270; default -> 0; }));
            for (var bone : baked.topLevelBones()) mesh.add(bone, CONTROLLER, false, front, pose);
        }
        LOADED.put(model, mesh);
        return mesh;
    }

    /** Whether the renderer moves a bone itself: `sway_` bones swing in the wind and `float_<n>` bones drift on the
     *  model's water. */
    static boolean driven(String name) { return name.startsWith("sway_") || name.startsWith("float_"); }

    boolean hasWater() { return quads.values().stream().anyMatch(list -> list.stream().anyMatch(Quad::water)); }

    private void animate(GeoBone bone, Set<String> moved, boolean below) {
        boolean inside = below || moved.contains(bone.getName()) || driven(bone.getName());
        if (inside) animated.add(bone.getName());
        for (var child : bone.getChildBones()) animate(child, moved, inside);
    }

    /** The group a bone's name gives it, or its parent's. */
    private static int group(String name, int parent) {
        if (name.startsWith("section_")) return Integer.parseInt(name.substring(8, name.indexOf('_', 8)));
        return name.equals("controller") ? CONTROLLER : name.startsWith("lit_") ? LIT : name.startsWith("cold_") ? COLD : parent;
    }

    private void add(GeoBone bone, int parent, boolean water, Direction front, PoseStack pose) {
        if (animated.contains(bone.getName())) return;
        int group = group(bone.getName(), parent);
        water |= bone.getName().endsWith("water");
        pose.pushPose();
        RenderUtil.prepMatrixForBone(pose, bone);
        for (var cube : bone.getCubes()) add(cube, group, water, front, pose);
        for (var child : bone.getChildBones()) add(child, group, water, front, pose);
        pose.popPose();
    }

    private void add(GeoCube cube, int group, boolean water, Direction front, PoseStack pose) {
        pose.pushPose();
        RenderUtil.translateToPivotPoint(pose, cube); RenderUtil.rotateMatrixAroundCube(pose, cube); RenderUtil.translateAwayFromPivotPoint(pose, cube);
        var placed = new ArrayList<Quad>(); var centre = new Vector3f(); int corners = 0;
        for (var quad : cube.quads()) {
            if (quad == null) continue;
            var normal = pose.last().normal().transform(new Vector3f(quad.normal()));
            RenderUtil.fixInvertedFlatCube(cube, normal);
            var points = new Vector3f[4]; var uv = new float[8];
            for (int i = 0; i < 4; i++) {
                var v = quad.vertices()[i];
                var at = pose.last().pose().transform(new Vector4f(v.position().x(), v.position().y(), v.position().z(), 1));
                points[i] = new Vector3f(at.x(), at.y(), at.z()); centre.add(points[i]); corners++;
                uv[2 * i] = v.texU(); uv[2 * i + 1] = v.texV();
            }
            placed.add(new Quad(points, uv, normal, group == LIT && model.heated, water));
        }
        pose.popPose();
        if (placed.isEmpty()) return;
        centre.div(corners);
        int cell = cell(group, front, centre);
        if (cell == Integer.MIN_VALUE) return;
        var offset = cell == CONTROLLER_CELL ? BlockPos.ZERO : ModeledMachine.position(BlockPos.ZERO, front, model.cells.get(cell));
        var into = quads.computeIfAbsent(key(front, cell, group), k -> new ArrayList<>());
        for (var quad : placed) {
            for (var point : quad.corners()) point.sub(offset.getX(), offset.getY(), offset.getZ());
            into.add(quad);
        }
    }

    /** The cell a cube centred at `centre` is drawn from: the nearest that its section fills, or the controller's for
     *  the controller's own and the fire's bones; none for a section that fills no cell. */
    private int cell(int group, Direction front, Vector3f centre) {
        if (group < 0) return CONTROLLER_CELL;
        int best = Integer.MIN_VALUE; double nearest = Double.MAX_VALUE;
        for (int i = 0; i < model.cells.size(); i++) {
            if (!model.cells.get(i).pieces().containsKey(group)) continue;
            var p = ModeledMachine.position(BlockPos.ZERO, front, model.cells.get(i));
            double dx = Math.max(0, Math.max(p.getX() - centre.x, centre.x - p.getX() - 1)), dy = Math.max(0, Math.max(p.getY() - centre.y, centre.y - p.getY() - 1)),
                    dz = Math.max(0, Math.max(p.getZ() - centre.z, centre.z - p.getZ() - 1)), d = dx * dx + dy * dy + dz * dz;
            if (d < nearest) { nearest = d; best = i; }
        }
        return best;
    }
}
