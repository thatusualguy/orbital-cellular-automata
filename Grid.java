import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 2D grid of Cells.
 *
 * Topology:
 *   - Columns wrap (toroidal in the horizontal / orbital direction).
 *   - Rows do NOT wrap (hard boundary at Earth and at outer space).
 *
 * Invariant (enforced by rules, protected by Cell): at most one object per cell.
 */
public class Grid {

    private final int rows;
    private final int cols;
    private final Cell[][] cells;

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    public Grid(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("grid must be at least 1x1");
        }
        this.rows = rows;
        this.cols = cols;
        this.cells = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell(r, c);
            }
        }
    }

    // ------------------------------------------------------------------
    // Dimensions
    // ------------------------------------------------------------------

    public int rows() { return rows; }
    public int cols() { return cols; }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    /** Raw accessor. Caller must keep row in range. */
    public Cell get(int row, int col) {
        return cells[row][wrapCol(col)];
    }

    /** Row stays bounded; column wraps. */
    public boolean inBounds(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /** Wrap a column into [0, cols). Handles negative values. */
    public int wrapCol(int col) {
        int c = col % cols;
        return (c < 0) ? c + cols : c;
    }

    /** Clamp a row into [0, rows). */
    public int clampRow(int row) {
        if (row < 0) return 0;
        if (row >= rows) return rows - 1;
        return row;
    }

    // ------------------------------------------------------------------
    // Occupancy helpers
    // ------------------------------------------------------------------

    public boolean isEmpty(int row, int col) {
        return get(row, col).isEmpty();
    }

    public boolean isOccupied(int row, int col) {
        return get(row, col).isOccupied();
    }

    // ------------------------------------------------------------------
    // Neighbors
    // ------------------------------------------------------------------

    /**
     * The up-to-8 neighbors of (row, col) as {row, col} pairs.
     * Columns wrap, rows are bounded. Returned list excludes the center.
     */
    public List<int[]> neighbors8(int row, int col) {
        List<int[]> out = new ArrayList<int[]>(8);
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int r = row + dr;
                if (r < 0 || r >= rows) continue;      // rows do not wrap
                out.add(new int[]{ r, wrapCol(col + dc) });
            }
        }
        return out;
    }

    /**
     * The two horizontal neighbors of (row, col) as {row, col} pairs.
     * Columns wrap; row is fixed.
     */
    public List<int[]> neighborsLR(int row, int col) {
        List<int[]> out = new ArrayList<int[]>(2);
        out.add(new int[]{ row, wrapCol(col - 1) });
        out.add(new int[]{ row, wrapCol(col + 1) });
        return out;
    }

    // ------------------------------------------------------------------
    // Free-neighbor search (used by NudgeRule)
    // ------------------------------------------------------------------

    /**
     * Find an empty cell in the same row as (row, col), searching in the
     * order the nudge policy prefers.
     *
     * @param row      row of the blocked target cell
     * @param col      column of the blocked target cell
     * @param dir      mover's direction (+1 prograde, -1 retrograde)
     * @param behavior nudge strategy from Settings
     * @param rng      for NUDGE_RANDOM
     * @return {row, col} of a free cell, or null if the row is packed
     */
    public int[] findFreeNeighbor(int row, int col, int dir,
                                  Settings.PassBehavior behavior,
                                  Random rng) {
        switch (behavior) {
            case NUDGE_AHEAD: {
                int ahead = wrapCol(col + dir);
                if (isEmpty(row, ahead)) return new int[]{ row, ahead };
                // fall through to side search
                return firstEmpty(row, col, rng);
            }
            case NUDGE_RANDOM:
                return firstEmpty(row, col, rng);
            case STAY_PUT:
            case BOUNCE_BACK:
            default:
                return null;
        }
    }

    /**
     * Scan left/right (in that order, then random fallback) for any empty
     * cell in the same row. Returns null if the row is completely full.
     */
    private int[] firstEmpty(int row, int col, Random rng) {
        int left  = wrapCol(col - 1);
        int right = wrapCol(col + 1);
        if (isEmpty(row, left))  return new int[]{ row, left };
        if (isEmpty(row, right)) return new int[]{ row, right };
        // widen the search across the row so a packed local cluster
        // doesn't block a nudge when there is clearly free space elsewhere
        int[] scanned = scanRow(row, col);
        if (scanned != null) return scanned;
        return null;
    }

    /** Scan the whole row outward from col, returning the first empty cell. */
    private int[] scanRow(int row, int col) {
        for (int d = 2; d <= cols; d++) {
            int l = wrapCol(col - d);
            if (isEmpty(row, l)) return new int[]{ row, l };
            int r = wrapCol(col + d);
            if (isEmpty(row, r)) return new int[]{ row, r };
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Bulk operations
    // ------------------------------------------------------------------

    public void clear() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c].clear();
            }
        }
    }

    /** Advance every cell's near-miss flash timer by one frame. */
    public void tickFlashes() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c].tickFlash();
            }
        }
    }

    /** Count objects of a given type. Used by the HUD. */
    public int count(OrbitalObject.Type type) {
        int n = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                OrbitalObject o = cells[r][c].getObject();
                if (o != null && o.getType() == type) n++;
            }
        }
        return n;
    }

    public int countTotal() {
        return count(OrbitalObject.Type.SATELLITE) + count(OrbitalObject.Type.DEBRIS);
    }

    // ------------------------------------------------------------------
    // Debug
    // ------------------------------------------------------------------

    /**
     * Row-major text dump. Uses Cell.toString: '.' empty, 'S' satellite, 'D' debris.
     * Useful for headless rule tests; not used by the sketch.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                sb.append(cells[r][c].toString());
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
