/**
 * A single cell in the orbital automaton grid.
 *
 * Invariant (enforced by the simulation, not by this class):
 *   a cell holds at most one OrbitalObject at a time.
 *
 * The class also carries a short-lived "near miss" flash counter so the
 * renderer can show when two objects encountered each other but did not
 * collide (see spec §4.6).
 */
public class Cell {

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    /** The object occupying this cell, or null if empty. */
    private OrbitalObject object;

    /** Row and column are stored for convenience (projection, debugging). */
    private final int row;
    private final int col;

    /**
     * Countdown (in frames) for the near-miss flash overlay.
     * 0 means "no flash active".
     */
    private int nearMissFrames;

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.object = null;
        this.nearMissFrames = 0;
    }

    // ------------------------------------------------------------------
    // Occupancy queries
    // ------------------------------------------------------------------

    public boolean isEmpty() {
        return object == null;
    }

    public boolean isOccupied() {
        return object != null;
    }

    public OrbitalObject getObject() {
        return object;
    }

    /** Convenience: does this cell contain a satellite? */
    public boolean hasSatellite() {
        return object != null && object.getType() == OrbitalObject.Type.SATELLITE;
    }

    /** Convenience: does this cell contain debris? */
    public boolean hasDebris() {
        return object != null && object.getType() == OrbitalObject.Type.DEBRIS;
    }

    // ------------------------------------------------------------------
    // Occupancy mutation
    // ------------------------------------------------------------------

    /**
     * Place an object in this cell.
     *
     * @throws IllegalStateException if the cell is already occupied.
     *         The simulation must clear or move the existing object first —
     *         this is how the "one object per cell" invariant is enforced.
     */
    public void set(OrbitalObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("use clear() to empty a cell");
        }
        if (this.object != null) {
            throw new IllegalStateException(
                "Cell (" + row + "," + col + ") already occupied by " + this.object);
        }
        this.object = obj;
    }

    /**
     * Remove and return the object in this cell, or null if empty.
     * Used by the movement rule when an object leaves.
     */
    public OrbitalObject take() {
        OrbitalObject out = this.object;
        this.object = null;
        return out;
    }

    /** Remove whatever is in this cell without returning it. */
    public void clear() {
        this.object = null;
    }

    // ------------------------------------------------------------------
    // Near-miss flash (visual only — never affects simulation)
    // ------------------------------------------------------------------

    /**
     * Trigger a near-miss flash. Duration in frames comes from Settings
     * (see Settings.NEAR_MISS_FLASH_FRAMES).
     */
    public void flashNearMiss(int frames) {
        if (frames > this.nearMissFrames) {
            this.nearMissFrames = frames;
        }
    }

    /** True while the flash overlay should still be drawn. */
    public boolean isFlashing() {
        return nearMissFrames > 0;
    }

    /** 0.0 .. 1.0 — 1.0 immediately after the flash, fading to 0. */
    public float flashAlpha(int totalFrames) {
        if (nearMissFrames <= 0 || totalFrames <= 0) return 0f;
        return (float) nearMissFrames / (float) totalFrames;
    }

    /** Advance the flash timer by one frame. Called once per simulation tick. */
    public void tickFlash() {
        if (nearMissFrames > 0) nearMissFrames--;
    }

    // ------------------------------------------------------------------
    // Geometry accessors
    // ------------------------------------------------------------------

    public int row() { return row; }
    public int col() { return col; }

    // ------------------------------------------------------------------
    // Debug
    // ------------------------------------------------------------------

    @Override
    public String toString() {
        String occupant = (object == null) ? "." :
            (object.getType() == OrbitalObject.Type.SATELLITE ? "S" : "D");
        return occupant;
    }
}
