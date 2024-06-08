package ch.jdtt.BurierRaid;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.json.simple.JSONObject;

public class FactionRaid {
    private String faction;
    private String facID;
    private String totemUUID;
    private TotemLocation location;
    private Boolean isInWar;

    public FactionRaid(String faction, String facID,
                       String totemUUID, Boolean isInRaid,
                       Double x, Double y, Double z) {
        this.faction = faction;
        this.facID = facID;
        this.totemUUID = totemUUID;
        this.location = new TotemLocation(x, y, z);
        this.isInWar = isInRaid;
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