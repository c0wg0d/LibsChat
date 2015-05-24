package me.libraryaddict.Chat;

import me.libraryaddict.Hungergames.Types.HungergamesApi;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Map;

public class ChannelListeners implements Listener, PluginMessageListener {

    private Main main;

    public static Permission perms = null;
    public static Chat chat = null;


    public ChannelListeners(Main main) {
        this.main = main;
    }

    private void chatChannel(ChatChannel channel, Player player, String format, String message, String shortcutMessage) {
        RegisteredServiceProvider<Permission> rspp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rspp.getProvider();

        RegisteredServiceProvider<Chat> rspc = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        chat = rspc.getProvider();

        String sender = (channel.useDisplayNames() ? player.getDisplayName() : player.getName());
        format = (channel.getFormat() != null ? channel.getFormat() : format);
        String spyformat = "";
        spyformat = (channel.getSpyFormat() != null ? channel.getSpyFormat() : spyformat);
        if (channel.isCrossServer()) {
            sendData(channel.getName(), sender, format, spyformat, message);
        }
        //String formattedMessage = ChatColor.translateAlternateColorCodes('&', format.replace("{prefix}", PermissionsEx.getUser(player).getPrefix()).replace("{suffix}", PermissionsEx.getUser(player).getSuffix()).replace("{name}", sender).replace("{message}", message));
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', format.replace("{prefix}", chat.getPlayerPrefix(player)).replace("{suffix}", chat.getPlayerSuffix(player)).replace("{name}", sender));

        if (format.contains("{jobs}")) {
            String jobtitle = "&aEasy";
//
//            if (Bukkit.getPluginManager().isPluginEnabled("Jobs")) {
//            //if (Bukkit.getPluginManager().getPlugin("Jobs").isEnabled()) {
//                JobsPlayer jPlayer = Jobs.getPlayerManager().getJobsPlayer(player);
//                if (jPlayer != null)
//                    jobtitle = jPlayer.getDisplayHonorific();
//                if (jobtitle.isEmpty())
//                    jobtitle = "&8Unemployed";
//            }
            formattedMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage.replace("{jobs}", jobtitle));
        }

        if (format.contains("{title}")) {
            boolean titleFound = false;
            String defaultTitle = "";

            for (Map<?, ?> titleList : channel.channelTitles) {
                for (Map.Entry<?, ?> title : titleList.entrySet()) {
                    if (perms.playerHas(player, "rank." + title.getKey())) {
                        formattedMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage.replace("{title}", title.getValue().toString()));
                        titleFound = true;
                    } else if (title.getKey().equals("default")) {
                        defaultTitle = (String) title.getValue();
                    }
                }
            }
            if (!titleFound) {
                formattedMessage = ChatColor.translateAlternateColorCodes('&', formattedMessage.replace("{title}", defaultTitle));
            }
        }

        formattedMessage = formattedMessage.replace("{message}", message);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (channel.getRadius() < 0 || p.getLocation().distance(player.getLocation()) <= channel.getRadius()) {
                if (main.isChatSpy(p)) {
                    //String spyMessage = spyformat.replace("{prefix}", PermissionsEx.getUser(player).getPrefix()).replace("{suffix}", PermissionsEx.getUser(player).getSuffix()).replace("{name}", sender).replace("{message}", message);
                    String spyMessage = spyformat.replace("{prefix}", chat.getPlayerPrefix(player)).replace("{suffix}", chat.getPlayerSuffix(player)).replace("{name}", sender).replace("{message}", message);
                    spyMessage = ChatColor.translateAlternateColorCodes('&', spyMessage);
                    p.sendMessage(spyMessage);
                } else if (Bukkit.getPluginManager().isPluginEnabled("LibsHungergames")) {
                    if (HungergamesApi.getPlayerManager().getGamer(sender).isAlive()) {
                        p.sendMessage(formattedMessage);
                    } else if (!HungergamesApi.getPlayerManager().getGamer(sender).isAlive() && !HungergamesApi.getPlayerManager().getGamer(p).isAlive()) {
                        p.sendMessage(formattedMessage);
                    }
                } else if (main.getChatChannel(p) == channel
                        || (channel.useHearPermission() && p.hasPermission(channel.getPermissionToHear()))) {
                    p.sendMessage(formattedMessage);
                } else {
                    if (p == player && shortcutMessage != null) {
                        p.sendMessage(ChatColor.stripColor(shortcutMessage));
                    }
                }
            }
        }
        System.out.print(formattedMessage);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(@SuppressWarnings("deprecation") PlayerChatEvent event) {
        RegisteredServiceProvider<Permission> rspp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rspp.getProvider();

        RegisteredServiceProvider<Chat> rspc = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        chat = rspc.getProvider();

        if (event.isCancelled() || event.getRecipients().isEmpty())
            return;
        ChatChannel channel = main.getChatChannel(event.getPlayer());
        Main.ChannelShortcut shortcut = main.getShortcut(event.getMessage());
        if (shortcut != null) {
            if (shortcut.getPermission() != null && !event.getPlayer().hasPermission(shortcut.getPermission())) {
                event.getPlayer().sendMessage(ChatColor.RED + "You do not have access to this channel!");
                event.setCancelled(true);
                return;
            }
            channel = shortcut.getChannel();
            event.setMessage(event.getMessage().substring(shortcut.getKey().length()).trim());
        }
        if (Bukkit.getPluginManager().isPluginEnabled("LibsHungergames")) {
            if (!HungergamesApi.getPlayerManager().getGamer(event.getPlayer().getName()).isAlive() || main.getAliveChannel() == null) {
                channel = main.getDefaultChannel();
            } else {
                channel = main.getAliveChannel();
            }
        }
        if (channel != null) {
            event.setCancelled(true);
            if (event.getMessage().trim().length() <= 0) {
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot send an empty message!");
                return;
            }
            chatChannel(channel, event.getPlayer(), event.getFormat(), event.getMessage(),
                    shortcut != null && shortcut.getChannel() == channel ? shortcut.getMessage() : null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        RegisteredServiceProvider<Permission> rspp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rspp.getProvider();

        RegisteredServiceProvider<Chat> rspc = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
        chat = rspc.getProvider();

        Main.ChannelShortcut shortcut = main.getShortcut(event.getMessage());
        if (shortcut != null && !event.isCancelled()) {
            event.setCancelled(true);
            if (shortcut.getPermission() != null && !perms.has(event.getPlayer(), shortcut.getPermission())) {
                event.getPlayer().sendMessage(ChatColor.RED + "You do not have access to this channel!");
                return;
            }
            String msg = event.getMessage().substring(shortcut.getKey().length());
            if (msg.length() == 0) {
                event.getPlayer().sendMessage(ChatColor.RED + "You cannot send an empty message!");
                return;
            }
            @SuppressWarnings({"deprecation", "unchecked"}) PlayerChatEvent chatEvent = new PlayerChatEvent(event.getPlayer(), msg, "<%s> %s", new HashSet());
            Bukkit.getPluginManager().callEvent(chatEvent);
            if (!chatEvent.isCancelled()) {
                chatChannel(shortcut.getChannel(), event.getPlayer(), "<%s> %s", chatEvent.getMessage(), shortcut.getMessage());
            }
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        RegisteredServiceProvider<Permission> rspp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
        perms = rspp.getProvider();

        if (main.getDefaultChannel() != null) {
            main.addToChannel(main.getDefaultChannel(), event.getPlayer());
        }
        if ((perms.has(event.getPlayer(), "channel.admin.*")) && !main.isChatSpy(event.getPlayer()))
            main.addToChatSpies(event.getPlayer());
    }

    @Override
    public void onPluginMessageReceived(String pluginChannel, Player whoGivesAShitAboutThePlayer, byte[] bytes) {
        if (!pluginChannel.equals("BungeeCord")) {
            return;
            // This is not the channel we are looking for..
        }
        try {
            RegisteredServiceProvider<Permission> rspp = Bukkit.getServer().getServicesManager().getRegistration(Permission.class);
            perms = rspp.getProvider();

            RegisteredServiceProvider<Chat> rspc = Bukkit.getServer().getServicesManager().getRegistration(Chat.class);
            chat = rspc.getProvider();

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            String subchannel = in.readUTF();
            short len = in.readShort();
            byte[] msgbytes = new byte[len];
            in.readFully(msgbytes);
            DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
            if (subchannel.equals("LibrarysChat")) {
                long timestamp = msgin.readLong();
                if (timestamp >= System.currentTimeMillis()) {

                    String channelName = msgin.readUTF();
                    String sender = msgin.readUTF();
                    String format = msgin.readUTF();
                    String spyformat = msgin.readUTF();
                    String message = msgin.readUTF();
                    for (ChatChannel channel : main.getChannels()) {
                        if (channel.getName().equals(channelName) && channel.isCrossServer()) {
                            OfflinePlayer player1 = Bukkit.getOfflinePlayer(sender);
                            String prefix = "";
                            String suffix = "";
                            if (player1 != null) {
                                prefix = chat.getPlayerPrefix(null, player1);
                                suffix = chat.getPlayerSuffix(null, player1);
                            }
                            String chatMessage = format.replace("{prefix}", prefix)
                                    .replace("{suffix}", suffix)
                                    .replace("{name}", sender)
                                    .replace("{message}", message)
                                    .replace("{title}", "")
                                    .replace("{jobs}", "&aEasy");
                            chatMessage = ChatColor.translateAlternateColorCodes('&', chatMessage);
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (main.isChatSpy(player)) {
                                    String spyMessage = spyformat.replace("{prefix}", prefix)
                                            .replace("{suffix}", suffix)
                                            .replace("{name}", sender)
                                            .replace("{message}", message);
                                    spyMessage = ChatColor.translateAlternateColorCodes('&', spyMessage);
                                    player.sendMessage(spyMessage);
                                } else if (main.getChatChannel(player) == channel
                                        || (channel.useHearPermission() && player.hasPermission(channel.getPermissionToHear()))) {
                                    player.sendMessage(chatMessage);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ChatChannel hisChannel = main.getChatChannel(event.getPlayer());
        if (hisChannel != null) {
            main.removeFromChannel(hisChannel, event.getPlayer());
        }
        main.getChatManager().removeChatter(event.getPlayer().getName());
    }

    private void sendData(String channel, String sender, String format, String spyformat, String message) {
        try {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
            out.writeUTF("Forward");
            out.writeUTF("ALL");
            out.writeUTF("LibrarysChat");

            ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
            DataOutputStream msgout = new DataOutputStream(msgbytes);
            msgout.writeLong(System.currentTimeMillis() + 1000);
            msgout.writeUTF(channel);
            msgout.writeUTF(sender);
            msgout.writeUTF(format);
            msgout.writeUTF(spyformat);
            msgout.writeUTF(message);
            out.writeShort(msgbytes.toByteArray().length);
            out.write(msgbytes.toByteArray());
            Bukkit.getOnlinePlayers().iterator().next().sendPluginMessage(main, "BungeeCord", b.toByteArray());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
