package dev.civilization;

/** A controller block whose machine is a native model, which held previews need before any block entity exists. */
public interface ModeledBlock {
    /** The model this controller builds, or null for a variant without one. */
    MachineModel model();
}
