package me.libraryaddict.Chat.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.libraryaddict.Chat.Main;
import org.bukkit.entity.Player;
import ru.tehkode.permissions.bukkit.PermissionsEx;

/*
 * Created by cheracc on 6/27/2014.
 */

public class Chatspy implements CommandExecutor {
    private Main main;

    public Chatspy(Main main) {
        this.main = main;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

        if (PermissionsEx.getUser((Player) sender).has("group.moderator")) {
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
