import java.util.Random;

/**
 * One piece of orbital traffic: either an active satellite or a piece of debris.
 *
 * Fields are mutable because several rules adjust them in place:
 *   - NudgeRule (BOUNCE_BACK) reverses direction
 *   - future vertical-drift / decay rules may change speed
 *   - collision rule reads size and speed to compute yield
 *
 * The class holds no reference to the grid. Movement, collisions, and spawning
 * are the responsibility of Rule implementations.
 */
public class OrbitalObject {

    // ------------------------------------------------------------------
    // Type
    // ------------------------------------------------------------------

    public enum Type {
        SATELLITE,
        DEBRIS
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------

    private final Type type;

    /** Relative size, 0.1 .. 5.0 in the default Settings. */
    private float size;

    /** Absolute speed in cells per step (always >= 0). */
    private int speed;

    /** +1 prograde, -1 retrograde. */
    private int direction;
    
    private int age;
    private final int maxAge;   // -1 = never ages (satellites)

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    public OrbitalObject(Type type, float size, int speed, int direction) {
        this(type, size, speed, direction, -1);
    }

    public OrbitalObject(Type type, float size, int speed, int direction, int maxAge) {
        if (size <= 0f) throw new IllegalArgumentException("size must be > 0");
        if (speed < 0) throw new IllegalArgumentException("speed must be >= 0");
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("direction must be +1 or -1");
        }
        this.type      = type;
        this.size      = size;
        this.speed     = speed;
        this.direction = direction;
        this.maxAge    = maxAge;
        this.age       = 0;
    }
    
    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public Type getType()            { return type; }
    public float getSize()           { return size; }
    public int getSpeed()            { return speed; }
    public int getDirection()        { return direction; }

    public boolean isSatellite()     { return type == Type.SATELLITE; }
    public boolean isDebris()        { return type == Type.DEBRIS; }
   

    /** Signed speed, handy for movement math: speed * direction. */
    public int signedSpeed()         { return speed * direction; }

    public int  getAge()    { return age; }
    public int  getMaxAge() { return maxAge; }

    /** True if this object has a finite lifespan. */
    public boolean ages()   { return maxAge > 0; }

    public void incrementAge() { age++; }

    public boolean isExpired() { return ages() && age >= maxAge; }

    // ------------------------------------------------------------------
    // Mutators (used by rules)
    // ------------------------------------------------------------------

    public void setSize(float size) {
        if (size <= 0f) throw new IllegalArgumentException("size must be > 0");
        this.size = size;
    }

    public void setSpeed(int speed) {
        if (speed < 0) throw new IllegalArgumentException("speed must be >= 0");
        this.speed = speed;
    }

    public void setDirection(int direction) {
        if (direction != 1 && direction != -1) {
            throw new IllegalArgumentException("direction must be +1 or -1");
        }
        this.direction = direction;
    }

    /** Reverse prograde/retrograde. Used by NudgeRule when PASS_BEHAVIOR == BOUNCE_BACK. */
    public void flipDirection() {
        this.direction = -this.direction;
    }

    // ------------------------------------------------------------------
    // Factories (draw values from Settings ranges)
    // ------------------------------------------------------------------

    /** Random satellite using the ranges defined in Settings. */
    public static OrbitalObject randomSatellite(Settings s, Random rng) {
        float size  = lerp(s.SAT_SIZE_MIN,  s.SAT_SIZE_MAX,  rng.nextFloat());
        int   speed = randInt(rng, s.SAT_SPEED_MIN, s.SAT_SPEED_MAX);
        int   dir   = rng.nextBoolean() ? 1 : -1;
        return new OrbitalObject(Type.SATELLITE, size, speed, dir);
    }

    /** Random debris using the ranges defined in Settings. */
    public static OrbitalObject randomDebris(Settings s, Random rng) {
        float size  = lerp(s.DEB_SIZE_MIN,  s.DEB_SIZE_MAX,  rng.nextFloat());
        int   speed = randInt(rng, s.DEB_SPEED_MIN, s.DEB_SPEED_MAX);
        int   dir   = rng.nextBoolean() ? 1 : -1;
        int   maxAge = rollMaxAge(s, size, rng);
        return new OrbitalObject(Type.DEBRIS, size, speed, dir, maxAge);
    }
    
    /**
     * Debris spawned by a collision. Size and speed are scaled by the energy
     * of the impact so bigger/faster crashes produce chunkier fragments.
     * The spawner decides how many to create (see CollisionRule).
     */
    public static OrbitalObject collisionDebris(Settings s, Random rng,
                                                float sizeA, float sizeB,
                                                int speedA, int speedB) {
        float parentMass  = sizeA + sizeB;
        float parentSpeed = Math.abs(speedA) + Math.abs(speedB);
    
        float size = clamp(s.DEB_SIZE_MIN,
                           s.DEB_SIZE_MAX,
                           s.DEB_SIZE_MIN + parentMass * 0.25f * rng.nextFloat());
    
        int speed = randInt(rng, s.DEB_SPEED_MIN,
                            Math.min(s.DEB_SPEED_MAX,
                                     s.DEB_SPEED_MIN + Math.round(parentSpeed)));
        int dir = rng.nextBoolean() ? 1 : -1;
    
        int maxAge = rollMaxAge(s, size, rng);
        return new OrbitalObject(Type.DEBRIS, size, speed, dir, maxAge);
    }
    
    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float lo, float hi, float v) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Inclusive both ends. */
    private static int randInt(Random rng, int lo, int hi) {
        if (hi < lo) { int t = lo; lo = hi; hi = t; }
        return lo + rng.nextInt(hi - lo + 1);
    }
    
    /**
     * Lifetime scales with size:
     *   base = DEBRIS_AGE_BASE + size * DEBRIS_AGE_PER_SIZE
     *   jitter in [-J, +J] * base
     * Clamped to at least 1 step.
     */
    public static int rollMaxAge(Settings s, float size, Random rng) {
        float base   = s.DEBRIS_AGE_BASE + size * s.DEBRIS_AGE_PER_SIZE;
        float jitter = (rng.nextFloat() * 2f - 1f) * s.DEBRIS_AGE_JITTER * base;
        int   maxAge = Math.round(base + jitter);
        return Math.max(1, maxAge);
    }

    // ------------------------------------------------------------------
    // Debug
    // ------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("%s[s=%.2f v=%d dir=%+d]",
                type, size, speed, direction);
    }
}
