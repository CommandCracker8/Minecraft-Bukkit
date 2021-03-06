package network.server.servers.hub;

import network.customevents.player.InventoryItemClickEvent;
import network.server.util.EventUtil;
import network.server.util.ItemCreator;
import npc.NPCEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

public class ParkourNPC implements Listener {
	private static String name = null;
	private static Location endlessLocation = null;
	private static Location courseLocation = null;
	
	public ParkourNPC() {
		name = "Parkour";

		World world = Bukkit.getWorlds().get(0);
		endlessLocation = new Location(world, 1598.5, 5, -1262.5, -270.0f, 0.0f);
		courseLocation = new Location(world, 1598.5, 5, -1298.5, -270.0f, 0.0f);

		LivingEntity livingEntity = new NPCEntity(EntityType.SKELETON, "&e" + name, new Location(world, 1673.5, 5, -1291.5)) {
			@Override
			public void onInteract(Player player) {
				openParkourInventory(player);
			}
		}.getLivingEntity();
		livingEntity.getEquipment().setBoots(new ItemCreator(Material.DIAMOND_BOOTS).setGlow(true).getItemStack());

		new NPCEntity(EntityType.SKELETON, "&eTo Spawn", new Location(world, 1597.5, 5, -1264.5), endlessLocation) {
			@Override
			public void onInteract(Player player) {
				player.teleport(Events.getSpawn());
			}
		};

		new NPCEntity(EntityType.SKELETON, "&eTo Spawn", new Location(world, 1597.5, 5, -1296.5), courseLocation) {
			@Override
			public void onInteract(Player player) {
				player.teleport(Events.getSpawn());
			}
		};

		EventUtil.register(this);
	}
	
	public static void openParkourInventory(Player player) {
		Inventory inventory = Bukkit.createInventory(player, 9 * 3, name);
		inventory.setItem(12, new ItemCreator(Material.GOLD_BOOTS).setName("&bEndless Parkour").getItemStack());
		inventory.setItem(14, new ItemCreator(Material.CHAINMAIL_BOOTS).setName("&bParkour Course").getItemStack());
		player.openInventory(inventory);
	}

	public static Location getEndlessLocation() {
		return endlessLocation;
	}
	
	@EventHandler
	public void onInventoryItemClick(InventoryItemClickEvent event) {
		if(event.getTitle().equals(name)) {
			Player player = event.getPlayer();
			if(event.getItem().getType() == Material.GOLD_BOOTS) {
				player.teleport(endlessLocation);
			} else {
				player.teleport(courseLocation);
			}
			event.setCancelled(true);
		}
	}
}