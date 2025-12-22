//Developed by _ItsAndrew_
package me.andrew.DiscordUtils.Plugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

//Helps me create the .yml files :)
public class YMLFiles {
    private final JavaPlugin plugin;
    private final String fileName;
    private File file;
    private FileConfiguration config;

    public YMLFiles(JavaPlugin plugin, String fileName){
        this.plugin = plugin;
        this.fileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        createFile();
    }

    private void createFile(){
        file = new File(plugin.getDataFolder(), fileName);
        if(!file.exists()){
            plugin.saveResource(fileName, false);
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig(){
        return config;
    }

    public void saveConfig(){
        try{
            config.save(file);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
