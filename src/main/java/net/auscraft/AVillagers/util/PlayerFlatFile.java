package net.auscraft.AVillagers.util;

import lombok.Getter;
import net.auscraft.AVillagers.AVillagers;
import org.bukkit.Bukkit;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by OhBlihv (Chris) on 9/01/2016.
 * This file is part of a project created for DBounty
 */
public class PlayerFlatFile extends FlatFile
{

	private static PlayerFlatFile instance = null;
	public static PlayerFlatFile getInstance()
	{
		if(instance == null)
		{
			instance = new PlayerFlatFile();
		}
		return instance;
	}

	@Getter
	private static final ConcurrentHashMap<UUID, Integer> costMap = new ConcurrentHashMap<>();

	private PlayerFlatFile()
	{
		super("players.yml");

		load();

		Bukkit.getScheduler().runTaskTimerAsynchronously(AVillagers.getInstance(), new Runnable()
		{

			@Override
			public void run()
			{
				save();
			}

		}, 36000L, 36000L);
	}

	public void load()
	{
		costMap.clear();

		BASE64Decoder base64Decoder = new BASE64Decoder();

		try
		{
			for(String uuidString : save.getStringList("players"))
			{
				try
				{
					//UUIDs are stored without the useless '==' on the end added by a base64 encoding. They can be added back on here.
					costMap.put(fromBytes(base64Decoder.decodeBuffer(uuidString.split(":")[0].concat("=="))), Integer.parseInt(uuidString.split(":")[1]));
				}
				catch(NumberFormatException e)
				{
					BUtil.logError("Invalid format received for '" + uuidString + " in players.yml. Skipping...");
				}
			}
		}
		catch(IOException e)
		{
			e.printStackTrace();
			BUtil.logError("Error loading UUIDs for ability disables.");
		}
	}

	public void save()
	{
		ArrayList<String> bountyStrings = new ArrayList<>(costMap.size());

		BASE64Encoder base64Encoder = new BASE64Encoder();

		//Encode as Base64, and trim off the unnecessary trailing ='s (These will be added on load)
		for(Map.Entry<UUID, Integer> entry : costMap.entrySet())
		{
			bountyStrings.add(base64Encoder.encode(toBytes(entry.getKey())).split("=")[0].concat(":").concat(String.valueOf(entry.getValue())));
		}
		saveValue("players", bountyStrings);
	}

	/*
	 * Copied from UUIDUtils.class to avoid NoClassDefErrors which occur rarely on Spigot.
	 */

	public static byte[] toBytes(UUID uuid)
	{
		ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
		byteBuffer.putLong(uuid.getMostSignificantBits());
		byteBuffer.putLong(uuid.getLeastSignificantBits());
		return byteBuffer.array();
	}

	public static UUID fromBytes(byte[] array)
	{
		if (array.length != 16)
		{
			throw new IllegalArgumentException("Illegal byte array length: " + array.length);
		}

		ByteBuffer byteBuffer = ByteBuffer.wrap(array);
		long mostSignificant = byteBuffer.getLong();
		long leastSignificant = byteBuffer.getLong();

		return new UUID(mostSignificant, leastSignificant);
	}

}
