package com.github.sirblobman.api.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.sirblobman.api.utility.Validate;

import org.jetbrains.annotations.Nullable;

public final class UpdateManager {
    private static final String BASE_UPDATE_URL = "https://api.spigotmc.org/legacy/update.php?resource=%s";
    private static final String BASE_RESOURCE_URL = "https://www.spigotmc.org/resources/%s/";
    
    private final JavaPlugin plugin;
    private final Map<String, Long> pluginResourceMap;
    private final Map<String, String> spigotVersionCache;
    
    public UpdateManager(JavaPlugin plugin) {
        this.plugin = Validate.notNull(plugin, "plugin must not be null!");
        this.pluginResourceMap = new HashMap<>();
        this.spigotVersionCache = new HashMap<>();
    }
    
    /**
     * Register a plugin with this manager.
     *
     * @param plugin     The plugin that will be checked for updates.
     * @param resourceId The SpigotMC resource id for the plugin.
     */
    public void addResource(Plugin plugin, long resourceId) {
        String pluginName = plugin.getName();
        this.pluginResourceMap.put(pluginName, resourceId);
    }
    
    /**
     * Schedule an async task that will check every plugin registered in this manager.
     */
    public void checkForUpdates() {
        if(!isEnabled()) {
            printDisabledInformation();
            return;
        }
        
        BukkitScheduler scheduler = Bukkit.getScheduler();
        scheduler.runTaskLaterAsynchronously(this.plugin, this::internalCheckForUpdates, 1L);
    }
    
    @Nullable
    public String getSpigotVersion(Plugin plugin) {
        String pluginName = plugin.getName();
        return this.spigotVersionCache.getOrDefault(pluginName, null);
    }
    
    private void printDisabledInformation() {
        Logger logger = this.plugin.getLogger();
        logger.info("[Update Checker] The update checking feature is disabled.");
        logger.info("[Update Checker] No plugin update information is available.");
    }
    
    private void internalCheckForUpdates() {
        retrieveSpigotVersions();
        
        Set<String> pluginNameSet = this.spigotVersionCache.keySet();
        pluginNameSet.forEach(this::printUpdateInformation);
    }
    
    private void printUpdateInformation(String pluginName) {
        Long resourceId = this.pluginResourceMap.getOrDefault(pluginName, null);
        if(resourceId == null) return;
        
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin plugin = pluginManager.getPlugin(pluginName);
        if(plugin == null) return;
        
        PluginDescriptionFile pluginDescription = plugin.getDescription();
        String pluginVersion = pluginDescription.getVersion();
        String spigotVersion = this.spigotVersionCache.getOrDefault(pluginName, null);
        
        Logger logger = this.plugin.getLogger();
        logger.info(" ");
        
        if(spigotVersion == null) {
            logger.info("[Update Checker] Update check failed for plugin '" + pluginName + "'.");
            logger.info("[Update Checker] Please make sure the server has access to the internet.");
            return;
        }
        
        if(spigotVersion.equals(pluginVersion)) {
            logger.info("[Update Checker] There are no updates available for plugin '" + pluginName + "'.");
            return;
        }
        
        logger.info("[Update Checker] A possible update was found for plugin '" + pluginName + "'.");
        logger.info("[Update Checker] Current Version: " + pluginVersion);
        logger.info("[Update Checker] New Version: " + spigotVersion);
        logger.info("[Update Checker] Download Link: " + String.format(BASE_RESOURCE_URL, resourceId));
    }
    
    private boolean isEnabled() {
        FileConfiguration config = this.plugin.getConfig();
        return config.getBoolean("update-checker", false);
    }
    
    private void retrieveSpigotVersions() {
        Set<String> pluginNameSet = this.pluginResourceMap.keySet();
        pluginNameSet.forEach(this::retrieveSpigotVersion);
    }
    
    private void retrieveSpigotVersion(String pluginName) {
        try {
            Long resourceId = this.pluginResourceMap.getOrDefault(pluginName, null);
            if(resourceId == null) return;
            
            String updateUrlString = String.format(BASE_UPDATE_URL, resourceId);
            URL url = new URL(updateUrlString);
            
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
            httpURLConnection.setRequestMethod("GET");
            
            InputStream inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            
            String spigotVersion = bufferedReader.readLine();
            this.spigotVersionCache.put(pluginName, spigotVersion);
            
            bufferedReader.close();
            inputStreamReader.close();
            inputStream.close();
        } catch(IOException ex) {
            Logger logger = this.plugin.getLogger();
            logger.log(Level.WARNING, "Update check failed for plugin '" + pluginName + "' because an error occurred:", ex);
        }
    }
}
