package ch.jdtt.autocompeter;

import ch.jdtt.BurierRaid.BurierRaid;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.struct.Relation;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import java.util.ArrayList;
import java.util.List;

public class startWarTabCompleter implements TabCompleter {
    BurierRaid plugin;
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] strings) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("startWar")){
            return null;
        }
        FPlayer fplayer = FPlayers.getInstance().getByPlayer(Bukkit.getPlayer(sender.getName()));
        List<String> enemies = new ArrayList<>();
        if (!fplayer.hasFaction()) {
            return null;
        }
        Faction faction = fplayer.getFaction();
        if (faction.getRelationCount(Relation.ENEMY) == 0) {
            return null;
        }
        Factions.getInstance().getAllFactions().forEach(relationFaction -> {
            if (faction.getRelationWish(relationFaction).isEnemy()) {
                enemies.add(relationFaction.getTag());
            }
        });
        return enemies;
    }
}
