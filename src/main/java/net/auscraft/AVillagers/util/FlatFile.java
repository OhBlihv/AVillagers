package net.auscraft.AVillagers.util;

import net.auscraft.AVillagers.AVillagers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by OhBlihv (Chris) on 9/01/2016.
 * This file is part of a project created for AVillagers
 */

public class FlatFile
{

	protected File saveFile = null;
	protected FileConfiguration save = null;

	volatile static FlatFile instance = null;
	static JavaPlugin plugin = null;
	
	String fileName = "config.yml";
	
	public static FlatFile getInstance()
	{
		if(instance == null)
		{
			instance = new FlatFile();
		}
		return instance;
	}

	//Default to config.yml. Allow other filenames through the other constructor
	FlatFile()
	{
		this("config.yml");
	}

	FlatFile(String fileName)
	{
		this.fileName = fileName;
		plugin = AVillagers.getInstance();
		saveDefaultConfig();
		getSave();
	}

	public FileConfiguration getSave() 
	{
	    if (save == null)
	    {
	        reloadFile();
	    }
	    return save;
	}
	
	public void reloadFile() 
	{
	    if (saveFile == null) 
	    {
	    	saveFile = new File(plugin.getDataFolder(), fileName);
	    }
	    save = YamlConfiguration.loadConfiguration(saveFile);
	 
	    // Look for defaults in the jar
	    Reader defConfigStream = null;
		try 
		{
			defConfigStream = new InputStreamReader(plugin.getResource(fileName), "UTF8");
		} 
		catch (UnsupportedEncodingException e) 
		{
			e.printStackTrace();
		}
	    if (defConfigStream != null)
	    {
	        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
	        save.setDefaults(defConfig);
	    }
	}
	
	public void saveDefaultConfig() 
	{
	    if (saveFile == null)
	    {
	        saveFile = new File(plugin.getDataFolder(), fileName);
	    }
	    if (!saveFile.exists())
	    {            
	    	plugin.saveResource(fileName, false);
	    }
	}
	
	public void saveToFile() 
	{
	    if (save == null || saveFile == null) 
	    {
	        return;
	    }
	    try
	    {
	        save.save(saveFile);
	    }
	    catch (IOException ex) 
	    {
	    	BUtil.logError("Could not save config to " + saveFile);
	    }
	}
	
	public Set<String> getChildren(String path)
	{
		return save.getConfigurationSection(path).getKeys(false);
	}

	public String getString(String path)
	{
		return getString(path, "");
	}

	public String getString(String path, String defaultString)
	{
		return save.getString(path, defaultString);
	}
	
	public List<String> getStringList(String path)
	{
		return save.getStringList(path);
	}
	
	public int getInt(String path)
	{
		return save.getInt(path);
	}

	public boolean getBoolean(String path)
	{
		return getBoolean(path, false);
	}

	public boolean getBoolean(String path, boolean defaultBoolean)
	{
		return save.getBoolean(path, defaultBoolean);
	}
	
	public long getLong(String path)
	{
		return save.getLong(path);
	}
	
	public void saveValue(String path, Object value)
	{
		save.set(path, value);
		saveToFile();
	}

	public double getDouble(String path)
	{
		return save.getDouble(path);
	}
	
	public float getFloat(String path)
	{
		return (float) save.getDouble(path);
	}

	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> getListMap(String path)
	{
		return (List<Map<String, Object>>) save.get(path);
	}

	public ConfigurationSection getConfigurationSection(String path)
	{
		return save.getConfigurationSection(path);
	}

	private static final Location DEFAULT_LOCATION = new Location(Bukkit.getWorlds().get(0), 0, 128, 0);

	public Location getLocation(ConfigurationSection configurationSection)
	{
		if(configurationSection == null || configurationSection.getKeys(false).isEmpty())
		{
			return DEFAULT_LOCATION;
		}

		World world;
		if(configurationSection.contains("world"))
		{
			String worldName = configurationSection.getString("world", null);
			if(worldName != null && Bukkit.getWorld(worldName) != null)
			{
				world = Bukkit.getWorld(worldName);
			}
			else
			{
				BUtil.logError("World name '" + worldName + "' does not correspond to a valid world on this server. (" + configurationSection.getCurrentPath() + ")");
				return DEFAULT_LOCATION;
			}
		}
		else
		{
			world = Bukkit.getWorlds().get(0);
			BUtil.logError("No world defined in '" + configurationSection.getCurrentPath() + "'. Defaulting to " + world.getName() + ". Please define a world next time.");
		}
		double  x = configurationSection.getDouble("x", 0D),
				y = configurationSection.getDouble("y", 128D),
				z = configurationSection.getDouble("z", 0D);
		float   pitch = (float) configurationSection.getDouble("pitch", 0D),
				yaw = (float) configurationSection.getDouble("yaw", 0D);

		return new Location(world, x, y, z, yaw, pitch);
	}

}
