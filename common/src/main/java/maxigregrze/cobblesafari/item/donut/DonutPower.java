package maxigregrze.cobblesafari.item.donut;

public record DonutPower(
        String id,
        DonutMainFlavor flavor,
        int category,
        int typeNbr,
        int valueLevel1,
        int valueLevel2,
        int valueLevel3
) {}
