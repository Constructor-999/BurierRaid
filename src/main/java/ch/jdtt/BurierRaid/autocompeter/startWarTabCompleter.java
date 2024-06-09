package ch.jdtt.BurierRaid.autocompeter;

import ch.jdtt.BurierRaid.BurierRaid;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.RED + "You are NOT in a faction!");
            return null;
        }
        Faction faction = fplayer.getFaction();
        return enemies;
    }
}
