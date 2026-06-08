package maxigregrze.cobblesafari.csboss;

/**
 * State of a participant in a session (plan 100 § 4/11).
 */
public class ParticipantState {
    public boolean alive = true;
    public boolean inRadius = true;
    /** Dead / disconnected / out-of-dimension: permanently excluded. */
    public boolean discarded = false;
}
