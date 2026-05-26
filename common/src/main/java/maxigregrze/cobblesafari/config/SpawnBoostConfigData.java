package maxigregrze.cobblesafari.config;

public class SpawnBoostConfigData {
    public final EffectSettings effectSettings = new EffectSettings();
    public final DurationSettings durationSettings = new DurationSettings();

    public static class EffectSettings {
        public float shinyBoostMultiplier = 4.0f;
        public float superShinyBoostMultiplier = 8.0f;
        public float ultraShinyBoostMultiplier = 12.0f;
        public float sparklingGuaranteeWindowFraction = 0.6f;
        public float uncommonBoostMultiplier = 16.0f;
        public float rareBoostMultiplier = 16.0f;
        public float ultraRareBoostMultiplier = 16.0f;
        public float ultraBeastBoostMultiplier = 10.0f;
        public float paradoxBoostMultiplier = 10.0f;
        public float legendaryBoostMultiplier = 10.0f;
        public float mythicalBoostMultiplier = 10.0f;
        public float effectDistanceBlocks = 80.0f;

        public float alphaPowerLevel1BoostMultiplier = 11.0f;
        public float alphaPowerLevel2BoostMultiplier = 21.0f;
        public float alphaPowerLevel3BoostMultiplier = 51.0f;

        public float hiddenPowerLevel1BoostMultiplier = 2.0f;
        public float hiddenPowerLevel2BoostMultiplier = 3.0f;
        public float hiddenPowerLevel3BoostMultiplier = 4.0f;

        public int friendshipPowerLevel1Bonus = 50;
        public int friendshipPowerLevel2Bonus = 75;
        public int friendshipPowerLevel3Bonus = 100;

        public float capturePowerLevel1Multiplier = 1.10f;
        public float capturePowerLevel2Multiplier = 1.20f;
        public float capturePowerLevel3Multiplier = 1.30f;

        public float encounterPowerLevel1SpawnBonus = 0.10f;
        public float encounterPowerLevel2SpawnBonus = 0.25f;
        public float encounterPowerLevel3SpawnBonus = 0.50f;

        public int humongoPowerLevel1LevelDelta = 5;
        public int humongoPowerLevel2LevelDelta = 10;
        public int humongoPowerLevel3LevelDelta = 15;

        public int teensyPowerLevel1LevelLoss = 5;
        public int teensyPowerLevel2LevelLoss = 10;
        public int teensyPowerLevel3LevelLoss = 15;

        public double luckPowerLevel1LuckBonus = 1.0;
        public double luckPowerLevel2LuckBonus = 3.0;
        public double luckPowerLevel3LuckBonus = 5.0;

        public int salvagePowerLevel1ExtraRolls = 1;
        public int salvagePowerLevel2ExtraRolls = 2;
        public int salvagePowerLevel3ExtraRolls = 3;

        public float partyBattleStatPowerLevel1Multiplier = 1.10f;
        public float partyBattleStatPowerLevel2Multiplier = 1.25f;
        public float partyBattleStatPowerLevel3Multiplier = 1.50f;
        public float moveResistanceEvBudgetFactor = 0.8f;

        public float selfAttackPowerLevel1DamageMultiplier = 1.10f;
        public float selfAttackPowerLevel2DamageMultiplier = 1.25f;
        public float selfAttackPowerLevel3DamageMultiplier = 1.50f;

        public float selfDefensePowerLevel1DamageTakenMultiplier = 0.90f;
        public float selfDefensePowerLevel2DamageTakenMultiplier = 0.75f;
        public float selfDefensePowerLevel3DamageTakenMultiplier = 0.50f;
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
