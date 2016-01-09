package net.auscraft.AVillagers;

import lombok.Getter;
import net.auscraft.AVillagers.util.BUtil;
import net.auscraft.AVillagers.util.FlatFile;
import net.auscraft.AVillagers.util.Messages;
import net.auscraft.AVillagers.util.PlayerFlatFile;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * Created by OhBlihv (Chris) on 9/01/2016.
 * This file is part of a project created for AVillagers
 */
public class AVillagers extends JavaPlugin implements Listener
{

	@Getter
	private static AVillagers instance = null;

	private ArrayDeque<String> aliasedCommands;

	private String permission;

	@Override
	public void onEnable()
	{
		instance = this;

		PlayerFlatFile.getInstance(); //Trigger loading of the players.yml

		reload();

		getServer().getPluginManager().registerEvents(this, this); //Register the PlayerCommandPreprocessEvent listener
	}

	@Override
	public void onDisable()
	{
		PlayerFlatFile.getInstance().save(); //Flush all player costs to file on shutdown
	}

	public void reload()
	{
		SpawnManager.reload();
		Messages.reloadMessages();

		FlatFile cfg = FlatFile.getInstance();

		//Reduce scope to avoid having a very temporary variable exposed
		{
			ArrayDeque<String> tempAliasedCommands = new ArrayDeque<>();
			for(String commandAlias : cfg.getStringList("options.command-aliases"))
			{
				//Reduce all aliases to lowercase to avoid case problems without using an equalsIgnoreCase() call every time-
				tempAliasedCommands.add(commandAlias.toLowerCase());
			}

			aliasedCommands = tempAliasedCommands;
		}

		permission = cfg.getString("options.permission", "avillagers.use");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if(command.getName().equalsIgnoreCase("avillagers"))
		{
			//I could have used a static final String here, but getting the command name makes this redundant.
			handleCommand(sender, command.getName(), args);
		}
		return true; //Return true every time to avoid Bukkit's ugly command syntax message
	}

	//Use a constant here to avoid creating a useless blank array for negating NPEs and showing the lack of arguments.
	private static final String[] BLANK_ARGS = new String[] {};

	@EventHandler(ignoreCancelled = true)
	public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent event)
	{
		if(aliasedCommands.contains(event.getMessage().substring(1).split(" ")[0].toLowerCase()))
		{
			event.setCancelled(true); //Cancel this event to treat it as a regular command
			String[] fullMessage = event.getMessage().substring(1).split(" "); //Substring to remove the initial '/'
			//                                      Copy all arguments but the first (if they exist). Else, there are no arguments to provide.
			handleCommand(event.getPlayer(), fullMessage[0].toLowerCase(), fullMessage.length > 1 ? Arrays.copyOfRange(fullMessage, 1, fullMessage.length) : BLANK_ARGS);
		}
	}

	/**
	 *
	 * This method is used to allow the base command and any aliased commands to
	 * act exactly as the other does.
	 *
	 * @param sender Sender of the command/alias. Should only be an instance of Player by the time it reaches this method.
	 * @param command Command typed. Only really useful for aliased commands, since the base command returns the same constant
	 * @param args Arguments supplied alongside the command.
	 */
	public void handleCommand(CommandSender sender, String command, String[] args)
	{
		//The only argument that is supported through console is "reload"
		if(!(sender instanceof Player) && !(args.length > 0 && args[0].equalsIgnoreCase("reload")))
		{
			//Hardcoded message that will only be seen by console. Non-issue.
			BUtil.printError(sender, "This command is only supported in-game.");
			return; //Quick-exit to remove any invalid cases early
		}

		if(!sender.hasPermission(permission))
		{
			Messages.sendMessage(sender, Messages.CMD_ERROR_NO_PERMISSION);
			return; //Quick-exit to remove any invalid cases early
		}

		if(args.length > 0)
		{
			if(args[0].equalsIgnoreCase("reload"))
			{
				reload();
				BUtil.printInfo(sender, "Reloaded Successfully.");
				return;
			}
		}
		else
		{
			SpawnManager.requestVillager((Player) sender, command);
			return;
		}

		Messages.sendMessage(sender, Messages.CMD_SYNTAX);
	}
}
