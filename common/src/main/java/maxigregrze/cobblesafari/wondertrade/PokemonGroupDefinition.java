package maxigregrze.cobblesafari.wondertrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PokemonGroupDefinition {
    private final String groupId;
    private final List<String> population;

    public PokemonGroupDefinition(String groupId, List<String> population) {
        this.groupId = groupId;
        this.population = Collections.unmodifiableList(new ArrayList<>(population));
    }

    public String getGroupId() {
        return groupId;
    }

    public List<String> getPopulation() {
        return population;
    }
}
