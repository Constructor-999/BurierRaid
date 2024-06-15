package ch.jdtt.BurierRaid;

public class FactionRaid {
    private String faction;
    private String totemUUID;
    private TotemLocation location;
    private Boolean isInWar;
    private String warHash;

    public FactionRaid(String faction,
                       String totemUUID, Boolean isInRaid,
                       String warHash,
                       Double x, Double y, Double z) {
        this.faction = faction;
        this.totemUUID = totemUUID;
        this.location = new TotemLocation(x, y, z);
        this.isInWar = isInRaid;
        this.warHash = warHash;
    }

    public Boolean getInWar() {
        return isInWar;
    }
    public String getTotemUUID() {
        return totemUUID;
    }
    public String getWarHash() {
        return warHash;
    }
}

class TotemLocation {
    private Double x;
    private Double y;
    private Double z;
    public TotemLocation(Double x, Double y, Double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}