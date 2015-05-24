package me.libraryaddict.Chat.Commands;

import me.libraryaddict.Chat.Main;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/*
 * Created by cheracc on 6/27/2014.
 */

public class Chatspy implements CommandExecutor {
    private Main main;

    public static Permission perms = null;

    public Chatspy(Main main) {
        this.main = main;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        RegisteredServiceProvider<Permission> rsp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();

        if (perms.has((Player) sender, "channel.admin.*")) {
            if (!sender.getName().equals("CONSOLE") && !main.isChatSpy((Player) sender)) {
                main.addToChatSpies((Player) sender);
                sender.sendMessage(ChatColor.GOLD + "ChatSpy has been enabled.");
            } else if (main.isChatSpy((Player) sender)) {
                main.removeFromChatSpies((Player) sender);
                sender.sendMessage(ChatColor.GOLD + "ChatSpy has been disabled.");
            }
        } else sender.sendMessage("Unknown command. Type \"/help\" for help.");

        return true;
    }
}
