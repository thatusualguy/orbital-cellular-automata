/**
 * Single source of truth for every tunable in the simulation.
 *
 * Rules read these; nothing else should hard-code a number. To change
 * behavior, edit a value here (or expose it via a key handler in the
 * main sketch).
 */
public class Settings {

    // ------------------------------------------------------------------
    // Grid
    // ------------------------------------------------------------------
    public int ROWS = 60;
    public int COLS = 1000;

    // ------------------------------------------------------------------
    // Visual / geometry
    // ------------------------------------------------------------------
    public float EARTH_RADIUS = 80f;
    public float ALT_MIN      = 40f;
    public float ALT_MAX      = 300f;

    // ------------------------------------------------------------------
    // Object spawn ranges (used by OrbitalObject factories)
    // ------------------------------------------------------------------
    public float SAT_SIZE_MIN  = 0.5f;
    public float SAT_SIZE_MAX  = 2.0f;
    public int   SAT_SPEED_MIN = 1;
    public int   SAT_SPEED_MAX = 3;

    public float DEB_SIZE_MIN  = 0.1f;
    public float DEB_SIZE_MAX  = 1.0f;
    public int   DEB_SPEED_MIN = 1;
    public int   DEB_SPEED_MAX = 4;

    // ------------------------------------------------------------------
    // Rule R3 — Satellite launch
    // ------------------------------------------------------------------
    public float LAUNCH_PROB    = 0.05f;
    public int   LAUNCH_ROW_MIN = 3;
    public int   LAUNCH_ROW_MAX = 800;
    public boolean LAUNCHING    = true;   // toggled by the on-screen button
    
    // ------------------------------------------------------------------
    // Rule R2 — Collision
    // ------------------------------------------------------------------
    public float COLLISION_BASE            = 0.005f;
    public float COLLISION_MAX             = 0.5f;
    public float DEBRIS_PER_COLLISION      = 2.0f;
    public float COLLISION_YIELD           = 1.0f;
    public int   MAX_DEBRIS_PER_COLLISION  = 8;
    public float SMALL_DEBRIS_SIZE         = 0.3f;
    
    // ------------------------------------------------------------------
    // Rule R6 — Nudge (non-colliding co-occupancy)
    // ------------------------------------------------------------------
    public PassBehavior PASS_BEHAVIOR         = PassBehavior.NUDGE_AHEAD;
    public boolean      FLASH_NEAR_MISS       = true;
    public int          NEAR_MISS_FLASH_FRAMES = 6;

    // ------------------------------------------------------------------
    // Rule R4 — Debris cleanup
    // ------------------------------------------------------------------
    public float CLEANUP_PROB = 0.02f;

    // ------------------------------------------------------------------
    // Rule R5 — Atmospheric decay
    // ------------------------------------------------------------------
    public float DECAY_PROB    = 0.001f;
    public int   DECAY_ROW_MAX = 1;
    
    // ------------------------------------------------------------------
    // Rule R7 — Debris aging (natural end-of-life removal)
    // ------------------------------------------------------------------
    public float DEBRIS_AGE_BASE     = 400f;   // base lifetime in steps at size 1
    public float DEBRIS_AGE_PER_SIZE = 300f;   // extra steps per unit of size
    public float DEBRIS_AGE_JITTER   = 0.5f;   // +/- fraction applied to base

    // ------------------------------------------------------------------
    // Simulation
    // ------------------------------------------------------------------
    public int RANDOM_SEED = 42;

    // ------------------------------------------------------------------
    // Nested enum — swap PASS_BEHAVIOR to change the nudge strategy
    // ------------------------------------------------------------------
    public enum PassBehavior {
        /** A slides past B in A's direction of travel. Default. */
        NUDGE_AHEAD,
        /** A moves to a random empty neighbor in the same row. */
        NUDGE_RANDOM,
        /** A does not move this step. */
        STAY_PUT,
        /** A reverses direction. */
        BOUNCE_BACK
    }

    // ------------------------------------------------------------------
    // Convenience: reset every field to its default.
    // Useful for the R key. Assigns the same values as above.
    // ------------------------------------------------------------------
    public void resetToDefaults() {
        ROWS = 32;
        COLS = 90;

        EARTH_RADIUS = 80f;
        ALT_MIN      = 40f;
        ALT_MAX      = 300f;

        SAT_SIZE_MIN  = 0.5f;   SAT_SIZE_MAX  = 2.0f;
        SAT_SPEED_MIN = 1;      SAT_SPEED_MAX = 3;
        DEB_SIZE_MIN  = 0.1f;   DEB_SIZE_MAX  = 1.0f;
        DEB_SPEED_MIN = 1;      DEB_SPEED_MAX = 4;

        LAUNCH_PROB    = 0.05f;
        LAUNCH_ROW_MIN = 2;
        LAUNCH_ROW_MAX = 6;

        COLLISION_BASE           = 0.05f;
        COLLISION_MAX            = 0.95f;
        DEBRIS_PER_COLLISION     = 2.0f;
        COLLISION_YIELD          = 1.0f;
        MAX_DEBRIS_PER_COLLISION = 8;

        PASS_BEHAVIOR          = PassBehavior.NUDGE_AHEAD;
        FLASH_NEAR_MISS        = true;
        NEAR_MISS_FLASH_FRAMES = 6;

        CLEANUP_PROB = 0.02f;

        DECAY_PROB    = 0.001f;
        DECAY_ROW_MAX = 1;
        
        DEBRIS_AGE_BASE     = 200f;
        DEBRIS_AGE_PER_SIZE = 150f;
        DEBRIS_AGE_JITTER   = 0.5f;
        
        LAUNCHING          = true;
        SMALL_DEBRIS_SIZE  = 0.3f;

        RANDOM_SEED = 42;
    }
}
