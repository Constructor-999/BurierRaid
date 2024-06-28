package ch.jdtt.Commands;

import ch.jdtt.BurierRaid.BurierRaid;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TestCommand implements CommandExecutor {
    BurierRaid plugin;
    public TestCommand(BurierRaid plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        String cmdName = command.getName();
        if (!cmdName.equalsIgnoreCase("claimReward")){
            return false;
        }
        return true;
    }
}
