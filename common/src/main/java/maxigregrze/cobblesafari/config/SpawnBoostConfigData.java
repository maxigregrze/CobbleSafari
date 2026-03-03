package maxigregrze.cobblesafari.config;

public class SpawnBoostConfigData {
    public final EffectSettings effectSettings = new EffectSettings();
    public final DurationSettings durationSettings = new DurationSettings();

    public static class EffectSettings {
        public float shinyBoostMultiplier = 4.0f;
        public float superShinyBoostMultiplier = 8.0f;
        public float ultraShinyBoostMultiplier = 12.0f;
        public float uncommonBoostMultiplier = 16.0f;
        public float rareBoostMultiplier = 16.0f;
        public float ultraRareBoostMultiplier = 16.0f;
        public float ultraBeastBoostMultiplier = 10.0f;
        public float paradoxBoostMultiplier = 10.0f;
        public float legendaryBoostMultiplier = 10.0f;
        public float mythicalBoostMultiplier = 10.0f;
        public float effectDistanceBlocks = 80.0f;
    }
    
    public static class DurationSettings {
        public int shinyBoostDuration = 6000;
        public int superShinyBoostDuration = 6000;
        public int ultraShinyBoostDuration = 6000;
        public int uncommonBoostDuration = 6000;
        public int rareBoostDuration = 6000;
        public int ultraRareBoostDuration = 6000;
        public int repelDuration = 6000;
        public int ultraBeastBoostDuration = 6000;
        public int paradoxBoostDuration = 6000;
        public int legendaryBoostDuration = 6000;
        public int mythicalBoostDuration = 6000;
    }
}
