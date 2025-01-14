package com.github.sirblobman.api.core.command;

import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.sirblobman.api.command.PlayerCommand;
import com.github.sirblobman.api.core.CorePlugin;
import com.github.sirblobman.api.nms.ItemHandler;
import com.github.sirblobman.api.nms.MultiVersionHandler;
import com.github.sirblobman.api.utility.ItemUtility;

public final class CommandItemToBase64 extends PlayerCommand {
    private final CorePlugin plugin;
    
    public CommandItemToBase64(CorePlugin plugin) {
        super(plugin, "item-to-base64");
        this.plugin = plugin;
    }
    
    @Override
    protected List<String> onTabComplete(Player player, String[] args) {
        return Collections.emptyList();
    }
    
    @Override
    protected boolean execute(Player player, String[] args) {
        ItemStack item = getHeldItem(player);
        if(ItemUtility.isAir(item)) {
            sendMessage(player, "error.invalid-held-item", null, true);
            return true;
        }
        
        MultiVersionHandler multiVersionHandler = this.plugin.getMultiVersionHandler();
        ItemHandler itemHandler = multiVersionHandler.getItemHandler();
        String base64String = itemHandler.toBase64String(item);
        
        player.sendMessage(base64String);
        return true;
    }
}
