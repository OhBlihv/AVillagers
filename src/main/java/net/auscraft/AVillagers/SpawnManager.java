package net.auscraft.AVillagers;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.auscraft.AVillagers.util.BUtil;
import net.auscraft.AVillagers.util.FlatFile;
import net.auscraft.AVillagers.util.Messages;
import net.auscraft.AVillagers.util.PlayerFlatFile;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.NumberFormat;
import java.util.ArrayDeque;

/**
 * Created by OhBlihv (Chris) on 9/01/2016.
 * This file is part of a project created for AVillagers
 */
public class SpawnManager
{

	private static Economy economy = null;
	private static WorldGuardPlugin worldGuardPlugin = null;

	private static NumberFormat numberFormat = NumberFormat.getCurrencyInstance();

	//Deques are very slightly faster than Lists for iteration, which is all we require.
	private static final ArrayDeque<String>     confirmationDeque = new ArrayDeque<>(),
												blacklistedRegions = new ArrayDeque<>();

	private static int      initialCost = 500;      //Initial cost of the villager
	private static double   costMultiplier = 2.0D;  //Double to multiply the above value (or stored value in PlayerFlatFile) by each purchase

	public static void reload()
	{
		FlatFile cfg = FlatFile.getInstance();

		initialCost = cfg.getInt("options.initial-cost");
		costMultiplier = cfg.getDouble("options.multiplier");

		blacklistedRegions.clear(); //Calling clear() on an empty Collection will return after a few small checks. No Worries.
		blacklistedRegions.addAll(cfg.getStringList("options.blacklisted-regions"));

		if(economy == null)
		{
			RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			if(economyProvider == null)
			{
				BUtil.logError("Vault not found! Cost options are not in effect.");
			}
			else
			{
				economy = economyProvider.getProvider();
			}
		}
		if(worldGuardPlugin == null)
		{
			Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
			if(plugin == null)
			{
				BUtil.logError("WorldGuard not found! Region Restrictions will not apply.");
			}
			else
			{
				worldGuardPlugin = (WorldGuardPlugin) plugin;
			}
		}
	}

	/**
	 * First attempt to use this method will check all restrictions, then if the player is in the confirmation deque.
	 * If they aren't, it will send them a message and add them to the deque.
	 *
	 * Second attempt to use this method will check all restrictions again (in-case the player moved or
	 * any circumstance changed between request and confirmation), print the spawn message and spawn the entity.
	 *
	 * @param player Player requesting the villager spawn
	 */
	public static void requestVillager(Player player, String command)
	{
		if(!checkBalance(player)) //Process this here to avoid checking in each path
		{
			return;
		}

		if(worldGuardPlugin != null && !blacklistedRegions.isEmpty())
		{
			for(ProtectedRegion region : worldGuardPlugin.getRegionManager(player.getWorld()).getApplicableRegions(player.getLocation()))
			{
				for(String blacklistedRegion : blacklistedRegions)
				{
					if(region.getId().equals(blacklistedRegion))
					{
						Messages.sendMessage(player, Messages.CMD_PURCHASE_ERROR_NO_REGION_ACCESS);
						return;
					}
				}
			}
		}

		if(confirmationDeque.contains(player.getName()))
		{
			confirmVillager(player);
		}
		else
		{
			confirmationDeque.add(player.getName());
			Messages.sendMessage(player, Messages.CMD_PURCHASE_CONFIRMATION
											.replace("{cost}", numberFormat.format(getSpawnCost(player)))
											.replace("{command}", command));
		}
	}

	private static void confirmVillager(Player player)
	{
		confirmationDeque.remove(player.getName());
		if(!chargePlayer(player)) //Take note, this also charges the player if it returns true.
		{
			return;
		}

		Villager villager = (Villager) player.getWorld().spawnEntity(player.getLocation(), EntityType.VILLAGER);
		if(villager == null)
		{
			player.sendMessage(ChatColor.RED + "An error occured while spawning your villager. You have not been charged.");

			//We still need to return the player's money here, IF they paid any to start with.
			if(economy != null)
			{
				economy.depositPlayer(player, getSpawnCost(player));
			}

			return;
		}

		Messages.sendMessage(player, Messages.CMD_PURCHASE_ON_SPAWN
				                             .replace("{cost}", numberFormat.format(getSpawnCost(player))));

		//Only add the price increase after they have been charged and we're sure the villager was spawned.
		try
		{
			//Instead of having two cases for a first run, use getOrDefault() to handle both at once.
			//Ugly cast to int. Multipliers should be rounded numbers, even though they're doubles. All responsibility is left to the configurer.
			PlayerFlatFile.getCostMap().put(player.getUniqueId(), (int) (PlayerFlatFile.getCostMap().getOrDefault(player.getUniqueId(), getSpawnCost(player)) * costMultiplier));
		}
		catch(NullPointerException e) //Always try/catch ConcurrentHashMaps, since they're terrible with Null keys/etc
		{
			BUtil.logError("An error occured while increasing " + player.getName() + "'s next villager spawn amount. The villager has been despawned.");
			player.sendMessage(ChatColor.RED + "An error occured while spawning your villager. You have not been charged.");

			villager.remove(); //Effectively 'kill' the entity without leaving any drops.

			//Return the players paid cash, if they paid any*
			if(economy != null)
			{
				economy.depositPlayer(player, getSpawnCost(player));
			}
		}
	}

	private static boolean checkBalance(Player player)
	{
		if(economy == null || economy.getBalance(player) >= getSpawnCost(player))
		{
			return true;
		}
		else //Economy is guaranteed to exist at this state. checkBalance returns true if Vault is not found
		{
			Messages.sendMessage(player, Messages.CMD_PURCHASE_ERROR_CANNOT_AFFORD
					                             .replace("{cost}", numberFormat.format(getSpawnCost(player)))
					                             .replace("{balance}", numberFormat.format(economy.getBalance(player))));
			return false;
		}
	}

	//Returns whether the player was charged
	private static boolean chargePlayer(Player player)
	{
		if(checkBalance(player))
		{
			//We still need to check if Vault still exists, since checkBalance() can return true if Vault isn't found.
			if(economy == null || economy.withdrawPlayer(player, getSpawnCost(player)).transactionSuccess())
			{
				return true;
			}
			else
			{
				player.sendMessage(ChatColor.RED + "An error occured while deducting money from your account.");
				//Case falls through to the false return below
			}
		}
		//Else case is handled within checkBalance(), and sends the player a message saying they cannot afford it.
		return false;
	}

	private static int getSpawnCost(Player player)
	{
		int spawnCost = initialCost;
		try
		{
			spawnCost = PlayerFlatFile.getCostMap().get(player.getUniqueId());
		}
		catch(NullPointerException e) //Player doesn't exist in the map -> Hasn't bought a villager before
		{
			//Do nothing. Price is already set at the initial cost.
		}
		return spawnCost;
	}

}
