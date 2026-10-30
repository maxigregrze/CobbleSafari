package maxigregrze.cobblesafari.csboss;

import net.minecraft.world.phys.Vec3;

/**
 * State of a participant in a session.
 */
public class ParticipantState {
    public boolean alive = true;
    public boolean inRadius = true;
    /** Dead / disconnected / out-of-dimension: permanently excluded. */
    public boolean discarded = false;
    /** Position mémorisée à la capture, cible du retour en fin de combat (plan 199 § 5.2). */
    public Vec3 startPos = Vec3.ZERO;
}
