package maxigregrze.cobblesafari.csboss;

/**
 * État d'un participant dans une session (plan 100 § 4/11).
 */
public class ParticipantState {
    public boolean alive = true;
    public boolean inRadius = true;
    /** Mort / déconnecté / hors-dimension : exclu définitivement. */
    public boolean discarded = false;
}
