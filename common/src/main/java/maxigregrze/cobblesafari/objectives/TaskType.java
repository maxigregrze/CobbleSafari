package maxigregrze.cobblesafari.objectives;

/**
 * The catalogue of dimensional-objective task types.
 *
 * <p>Each type declares which rolled variables it consumes ({@code count}, {@code species},
 * {@code type}, distinct-species {@code unique}) and whether it is {@code deferred} (reserved for
 * a future Cobblemon release and rejected by the datapack loader until then).
 */
public enum TaskType {
    TASK_LOOT("task_loot", true, false, false, false, false),
    TASK_CATCH_BASE("task_catch_base", true, false, false, false, false),
    TASK_CATCH_SPECIE("task_catch_specie", true, true, false, false, false),
    TASK_CATCH_TYPE("task_catch_type", true, false, true, false, false),
    TASK_CATCH_UNIQUE("task_catch_unique", true, false, false, true, false),
    TASK_CATCH_SHINY("task_catch_shiny", false, false, false, false, false),
    TASK_FIGHT_BASE("task_fight_base", true, false, false, false, false),
    TASK_FIGHT_SPECIE("task_fight_specie", true, true, false, false, false),
    TASK_FIGHT_TYPE("task_fight_type", true, false, true, false, false),
    TASK_FIGHT_UNIQUE("task_fight_unique", true, false, false, true, false),
    TASK_CSBOSS_BASE("task_csboss_base", false, false, false, false, false),
    TASK_CSBOSS_WIN("task_csboss_win", false, false, false, false, false),
    TASK_UNDERGROUND_BASE("task_underground_base", true, false, false, false, false),
    TASK_UNDERGROUND_PERFECT("task_underground_perfect", false, false, false, false, false),
    TASK_CSTRADER_BASE("task_cstrader_base", false, false, false, false, false),
    TASK_GIRATINACORE_BASE("task_giratinacore_base", false, false, false, false, false),
    // Reserved for Cobblemon 1.8 (alpha mechanic) — rejected by the loader until implemented.
    TASK_CATCH_ALPHA("task_catch_alpha", false, false, false, false, true),
    TASK_CATCH_ALPHASHINY("task_catch_alphashiny", false, false, false, false, true),
    TASK_FIGHT_ALPHA("task_fight_alpha", false, false, false, false, true);

    private final String id;
    private final boolean usesCount;
    private final boolean usesSpecies;
    private final boolean usesType;
    private final boolean usesUnique;
    private final boolean deferred;

    TaskType(String id, boolean usesCount, boolean usesSpecies, boolean usesType,
             boolean usesUnique, boolean deferred) {
        this.id = id;
        this.usesCount = usesCount;
        this.usesSpecies = usesSpecies;
        this.usesType = usesType;
        this.usesUnique = usesUnique;
        this.deferred = deferred;
    }

    public String id() {
        return id;
    }

    public boolean usesCount() {
        return usesCount;
    }

    public boolean usesSpecies() {
        return usesSpecies;
    }

    public boolean usesType() {
        return usesType;
    }

    public boolean usesUnique() {
        return usesUnique;
    }

    public boolean isDeferred() {
        return deferred;
    }

    /** Returns the type whose {@link #id()} matches (case-sensitive), or {@code null}. */
    public static TaskType byId(String id) {
        if (id == null) {
            return null;
        }
        for (TaskType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }
}
