package dev.civilization;

/** Exact solid volume on the existing construction grid; never rounds pieces into whole blocks. */
public record CargoVolume(long eighths) {
    public static final CargoVolume ZERO = new CargoVolume(0);

    public CargoVolume {
        if (eighths < 0) throw new IllegalArgumentException("Cargo volume cannot be negative");
    }

    public static CargoVolume pieces(long count, int eighthsPerPiece) {
        if (count < 0 || (eighthsPerPiece != 1 && eighthsPerPiece != 2
                && eighthsPerPiece != 4 && eighthsPerPiece != 8))
            throw new IllegalArgumentException("Expected a nonnegative count and full/half/quarter/eighth pieces");
        return new CargoVolume(Math.multiplyExact(count, eighthsPerPiece));
    }

    public CargoVolume plus(CargoVolume other) {
        return new CargoVolume(Math.addExact(eighths, other.eighths));
    }

    public double cubicMeters() { return eighths / 8d; }

    /** Whole pieces that fit; a store already over capacity accepts nothing. Does not mutate stock. */
    public long fittingPieces(CargoVolume capacity, int eighthsPerPiece, long offered) {
        pieces(offered, eighthsPerPiece); // Validate input and reject arithmetic overflow.
        long space = capacity.eighths > eighths ? capacity.eighths - eighths : 0;
        return Math.min(offered, space / eighthsPerPiece);
    }
}
