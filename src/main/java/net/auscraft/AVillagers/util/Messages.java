package net.auscraft.AVillagers.util;

import org.bukkit.command.CommandSender;

/**
 * Created by OhBlihv (Chris) on 9/01/2016.
 * This file is part of a project created for AVillagers
 */
public class Messages
{

	public static String    CMD_PURCHASE_CONFIRMATION,
							CMD_PURCHASE_ON_SPAWN,
							CMD_PURCHASE_ERROR_CANNOT_AFFORD,
							CMD_ERROR_NO_PERMISSION,
							CMD_PURCHASE_ERROR_NO_REGION_ACCESS,
							CMD_SYNTAX;

	public static void reloadMessages()
	{
		cfg = FlatFile.getInstance();

		CMD_PURCHASE_CONFIRMATION = loadString("messages.confirmation");
		CMD_PURCHASE_ON_SPAWN = loadString("messages.on-spawn");
		CMD_PURCHASE_ERROR_CANNOT_AFFORD = loadString("messages.error.cannot-afford");
		CMD_ERROR_NO_PERMISSION = loadString("messages.error.no-permission");
		CMD_PURCHASE_ERROR_NO_REGION_ACCESS = loadString("messages.error.no-region-access");
		CMD_SYNTAX = loadString("messages.error.syntax");
	}

	private static FlatFile cfg;

	private static String loadString(String path)
	{
		return BUtil.translateColours(cfg.getString(path));
	}

	/**
	 * Simple util method used to ignore blank messages, allowing the configurer to disable messages they don't want used
	 * @param sender The message receiver (CommandSender is used to send to console or player)
	 * @param message Message to send
	 */
	public static void sendMessage(CommandSender sender, String message)
	{
		if(message != null && message.length() > 0)
		{
			sender.sendMessage(message);
		}
	}

}
