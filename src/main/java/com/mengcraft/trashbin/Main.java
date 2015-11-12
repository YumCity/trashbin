package com.mengcraft.trashbin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created on 15-11-12.
 */
public class Main extends JavaPlugin implements Listener, Runnable {

    private final Map<UUID, Inventory> trashMap = new HashMap<UUID, Inventory>();

    private String title;
    private String messageSuccess;
    private String messageFull;

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        getServer().getScheduler().runTaskTimer(this, this,
                getConfig().getInt("clearDaily"),
                getConfig().getInt("clearDaily")
        );

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("trashbin").setExecutor(this);
    }

    @EventHandler
    public void handle(PlayerQuitEvent event) {
        trashMap.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handle(final PlayerDropItemEvent event) {
        final Inventory trashBin = getTrashBin(event.getPlayer());
        if (trashBin.addItem(event.getItemDrop().getItemStack()).isEmpty()) {
            if (trashBin.getViewers().size() != 0) {
                getServer().getScheduler().runTask(this, new Runnable() {
                    public void run() {
                        event.getPlayer().openInventory(trashBin); // Force refresh bin.
                    }
                });
            } else {
                event.getPlayer().sendMessage(getMessageSuccess());
            }
            event.getItemDrop().remove();
        } else {
            event.setCancelled(true);
            event.getPlayer().sendMessage(getMessageFull());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            openTrashBin(((Player) sender));
        }
        return args.length == 0;
    }

    public void run() {
        for (Map.Entry<UUID, Inventory> entry : trashMap.entrySet()) {
            entry.getValue().clear();
        }
    }

    private void openTrashBin(Player player) {
        player.openInventory(getTrashBin(player));
    }

    private Inventory getTrashBin(Player player) {
        Inventory trashBin = trashMap.get(player.getUniqueId());
        if (trashBin == null) {
            trashMap.put(player.getUniqueId(), trashBin = createFor(player));
        }
        return trashBin;
    }

    private Inventory createFor(Player player) {
        return getServer().createInventory(player, 54, getTitle());
    }

    public String getTitle() {
        if (title == null) {
            title = getConfig().getString("title");
        }
        return title;
    }

    public String getMessageFull() {
        if (messageFull == null) {
            messageFull = getConfig().getString("message.full");
        }
        return messageFull;
    }

    public String getMessageSuccess() {
        if (messageSuccess == null) {
            messageSuccess = getConfig().getString("message.success");
        }
        return messageSuccess;
    }

}
