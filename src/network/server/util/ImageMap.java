package network.server.util;

import network.player.account.AccountHandler.Ranks;
import network.server.CommandBase;
import network.server.DB;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

@SuppressWarnings("deprecation")
public class ImageMap {
	private static final int MAP_WIDTH = 128;
	private static final int MAP_HEIGHT = 128;
	private static boolean registeredCommand = false;
	private static List<ImageMap> imageMaps = null;
	private static Map<ItemFrame, Integer> itemFrameMaps = null;
	private ItemFrame itemFrame = null;
	private List<ItemFrame> itemFrames = null;
	private String name = null;
//	private String path = null;
	private BufferedImage image = null;
	private int width = 0;
	private int height = 0;
	
	public class CustomRender extends MapRenderer {
		private Image image = null;
		private boolean load = true;
		
		public CustomRender(BufferedImage image, int x1, int y1) {
			int x2 = MAP_WIDTH;
			int y2 = MAP_HEIGHT;
			if(x1 > image.getWidth() || y1 > image.getHeight()) {
				return;
			}
			if(x1 + x2 >= image.getWidth()) {
				x2 = image.getWidth() - x1;
			}
			if(y1 + y2 >= image.getHeight()) {
				y2 = image.getHeight() - y1;
			}
			this.image = image.getSubimage(x1, y1, x2, y2);
		}
		
		@Override
		public void render(MapView view, MapCanvas canvas, Player player) {
			if(image != null && load) {
				load = false;
				canvas.drawImage(0, 0, image);
			}
		}
	}
	
	public ImageMap(ItemFrame itemFrame, String name, BufferedImage image) {
		this(itemFrame, name, image, 5, 3);
	}
	
	public ImageMap(ItemFrame itemFrame, String name, BufferedImage image, int width, int height) {
		if(!registeredCommand) {
			registeredCommand = true;
			new CommandBase("reloadImageMaps") {
				@Override
				public boolean execute(CommandSender sender, String [] arguments) {
					for(ImageMap map : imageMaps) {
						map.execute();
					}
					return true;
				}
			}.setRequiredRank(Ranks.OWNER);
		}
		if(itemFrameMaps == null) {
			itemFrameMaps = new HashMap<ItemFrame, Integer>();
		}
		itemFrames = new ArrayList<ItemFrame>();
		this.itemFrame = itemFrame;
		this.name = name;
		this.image = image;
		this.width = width;
		this.height = height;
		execute();
		if(imageMaps == null) {
			imageMaps = new ArrayList<ImageMap>();
		}

		imageMaps.removeIf(new Predicate<ImageMap>() {
			@Override
			public boolean test(ImageMap map) {
				return map.getName().equals(name);
			}
		});

		imageMaps.add(this);
	}
	
	public void execute() {
		Location location = itemFrame.getLocation();
		BlockFace face = itemFrame.getFacing();
		int x1 = location.getBlockX();
		int y1 = location.getBlockY();
		int z1 = location.getBlockZ();
		for(int a = 0, counter = 0; a < height; ++a, --y1) {
			for(int b = 0; b < width; ++b) {
				int x = b * MAP_WIDTH;
				int y = a * MAP_HEIGHT;
				ItemFrame frame = getItemFrame(new Location(location.getWorld(), x1, y1, z1));
				int id;
				if(itemFrameMaps.containsKey(frame)) {
					id = itemFrameMaps.get(frame);
				} else {
					id = itemFrameMaps.size() + 1;
					itemFrameMaps.put(frame, id);
				}
				/* else if(DB.NETWORK_MAP_IDS.isKeySet("name", name)) {
					id = DB.NETWORK_MAP_IDS.getInt("name", name, "map_id");
					itemFrameMaps.put(frame, id);
				} else {
					id = DB.NETWORK_MAP_IDS.getSize() + 1;
					id += counter++;
					DB.NETWORK_MAP_IDS.insert("'" + name + "', '" + id + "'");
				}*/
				MapView mapView = Bukkit.getMap((short) id);
				while(mapView == null || mapView.getId() < id) {
					mapView = Bukkit.createMap(frame.getWorld());
				}
				for(MapRenderer renderer : mapView.getRenderers()) {
					mapView.removeRenderer(renderer);
				}
				mapView.addRenderer(new CustomRender(image, x, y));
				ItemStack map = new ItemStack(Material.MAP);
				map.setDurability(mapView.getId());
				frame.setItem(map);
				itemFrames.add(frame);
				switch(face) {
					case NORTH:
						--x1;
						break;
					case SOUTH:
						++x1;
						break;
					case EAST:
						--z1;
						break;
					case WEST:
						++z1;
						break;
					default:
						return;
				}
			}
			switch(face) {
				case NORTH:
				case SOUTH:
					x1 = location.getBlockX();
					break;
				case EAST:
				case WEST:
					z1 = location.getBlockZ();
					break;
				default:
					return;
			}
		}
	}
	
	public String getName() {
		return name;
	}
	
	public List<ItemFrame> getItemFrames() {
		return itemFrames;
	}
	
	public static ItemFrame getItemFrame(Location location) {
		for(Entity entity : location.getWorld().getEntities()) {
			if(entity instanceof ItemFrame) {
				Location loc = entity.getLocation();
				if(loc.getBlockX() == location.getBlockX() && loc.getBlockY() == location.getBlockY() && loc.getBlockZ() == location.getBlockZ()) {
					return (ItemFrame) entity;
				}
			}
		}
		return null;
	}
}
