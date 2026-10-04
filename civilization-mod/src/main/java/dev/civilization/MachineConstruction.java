package dev.civilization;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** Places real blueprint blocks from the held stack; the controller owns no hidden material inventory. */
public final class MachineConstruction {
    private record Pending(ServerPlayer player, BlockPos pos, double volume) {}
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<>();
    private MachineConstruction() {}

    static double takePlacedVolume(ServerPlayer player, BlockPos pos) {
        var pending = PENDING.get();
        if (pending == null || pending.player() != player || !pending.pos().equals(pos)) return 0;
        PENDING.remove();
        return pending.volume();
    }

    private static boolean supplies(ItemStack stack, MachineStructure.Part part) {
        if (part.material().equals("air")) return false;
        if (part.units() == 4)
            return stack.getItem() instanceof BlockItem block && !CuttingContent.piece(stack)
                    && MachineStructure.matches(block.getBlock().defaultBlockState(), part.material());
        return CuttingContent.piece(stack) && CuttingContent.units(stack) == part.units()
                && MachineStructure.matches(CuttingContent.material(stack), part.material());
    }

    /** A shared workshop cell may already contain a sibling piece while this part is still missing. */
    public static boolean present(Level level, BlockPos pos, MachineStructure.Part part, Direction front) {
        if (part.sharedMask() == 0) return MachineStructure.matches(level, pos, part, front);
        var state = level.getBlockState(pos);
        if (part.sharedMask() == 255 && CuttingContent.cuttable(state) && MachineStructure.matches(state, part.material())) return true;
        var cells = CutCells.read(level, pos);
        int required = CutCells.mask(CutBlock.bounds(MachineStructure.shape(part, front)));
        if ((CutCells.mask(cells) & required) != required) return false;
        for (int i = 0; i < 8; i++) if ((required & (1 << i)) != 0 && !MachineStructure.matches(cells[i], part.material())) return false;
        return true;
    }

    public static boolean blocked(Level level, BlockPos pos, MachineStructure.Part part, Direction front) {
        if (level.getBlockState(pos).isAir()) return false;
        if (part.sharedMask() == 0 || MachineStructure.obstructed(level, pos, part, front)) return true;
        int required = CutCells.mask(CutBlock.bounds(MachineStructure.shape(part, front)));
        return (CutCells.mask(CutCells.read(level, pos)) & required) != 0;
    }

    public static ItemInteractionResult useOn(ItemStack stack, BlockState state, Level level, BlockPos controller, Player player) {
        var parts = MachineStructure.guideParts(state);
        if (parts.isEmpty() || !hasMissing(level, controller, state.getValue(CivicBlock.FACING), parts, stack))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level instanceof ServerLevel server)
            build(server, controller, state.getValue(CivicBlock.FACING), parts, player, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean hasMissing(Level level, BlockPos controller, Direction front, List<MachineStructure.Part> parts, ItemStack stack) {
        for (var part : parts) {
            if (!supplies(stack, part)) continue;
            var pos = MachineStructure.position(controller, front, part);
            if (!level.hasChunkAt(pos) || !present(level, pos, part, front)) return true;
        }
        return false;
    }

    /** One click can place a held stack into its matching missing positions, from the blueprint's bottom upward. */
    public static int build(ServerLevel level, BlockPos controller, Direction front, List<MachineStructure.Part> parts, Player player, ItemStack stack) {
        if (!CivicAccess.allowed(level, controller, player)) return 0;
        int placed = 0;
        BlockPos firstBlocked = null, lastPlaced = null;
        for (var part : parts) {
            if (stack.isEmpty() || !supplies(stack, part)) continue;
            var pos = MachineStructure.position(controller, front, part);
            if (!level.hasChunkAt(pos)) { if (firstBlocked == null) firstBlocked = pos; continue; }
            if (present(level, pos, part, front)) continue;
            // Construction never clears terrain, liquids, other machines, or an occupied guide cell.
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || blocked(level, pos, part, front) || !level.getFluidState(pos).isEmpty()
                    || !CivicAccess.allowed(level, pos, player) || !level.mayInteract(player, pos)
                    || !player.mayUseItemAt(pos, Direction.UP, stack)
                    || AirshipSystem.at(level, pos) != null) {
                if (firstBlocked == null) firstBlocked = pos;
                continue;
            }
            BlockState state = MachineStructure.shape(part, front);
            BlockState[] joined = null;
            if (part.sharedMask() != 0) {
                joined = CutCells.read(level, pos);
                int mask = CutCells.mask(CutBlock.bounds(state));
                for (int i = 0; i < 8; i++) if ((mask & (1 << i)) != 0) joined[i] = CuttingContent.material(stack);
                state = CutCells.canonical(joined);
                if (state == null) state = CuttingContent.PIECE.get().defaultBlockState();
                if (state.is(CuttingContent.PIECE.get()) && state.equals(level.getBlockState(pos))) state = state.cycle(CutBlock.REVISION);
            } else if (part.units() == 4 && !part.material().endsWith("_port") && !part.material().equals("guardrail")
                    && !part.material().startsWith("engine_") && !part.material().equals("anvil") && !part.material().equals("arena_gate") && stack.getItem() instanceof BlockItem block)
                state = block.getBlock().defaultBlockState();
            var shape = part.units() == 4 ? state.getCollisionShape(level, pos)
                    : Shapes.create(CutBlock.bounds(MachineStructure.shape(part, front)));
            if (!level.isUnobstructed(null, shape.move(pos.getX(), pos.getY(), pos.getZ()))) {
                if (firstBlocked == null) firstBlocked = pos;
                continue;
            }
            var snapshot = BlockSnapshot.create(level.dimension(), level, pos);
            if (!level.setBlock(pos, state, 3)) { if (firstBlocked == null) firstBlocked = pos; continue; }
            if (level.getBlockEntity(pos) instanceof CutBlockEntity cut) {
                if (joined != null) cut.cells(joined);
                else if (part.units() < 4) cut.material(CuttingContent.material(stack));
            }
            if (player instanceof ServerPlayer server && part.units() < 4)
                PENDING.set(new Pending(server, pos.immutable(), CuttingContent.volume(part.units())));
            boolean cancelled;
            try { cancelled = EventHooks.onBlockPlace(player, snapshot, Direction.UP); }
            finally { PENDING.remove(); }
            if (cancelled) { snapshot.restore(); if (firstBlocked == null) firstBlocked = pos; continue; }
            stack.consume(1, player);
            level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE, pos);
            placed++;
            lastPlaced = pos;
        }
        if (lastPlaced != null) {
            var sound = level.getBlockState(lastPlaced).getSoundType(level, lastPlaced, player);
            level.playSound(null, lastPlaced, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1) / 2, sound.getPitch() * .8f);
        }
        if (firstBlocked != null)
            player.displayClientMessage(Component.literal("Construction blocked at " + firstBlocked.toShortString() + ". Clear the space or check access."), true);
        return placed;
    }
}
