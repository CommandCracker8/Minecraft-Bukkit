package network.gameapi.games.onevsones;

import network.Network;
import network.ProPlugin;
import network.customevents.TimeEvent;
import network.customevents.player.InventoryItemClickEvent;
import network.customevents.player.MouseClickEvent;
import network.customevents.player.PlayerLeaveEvent;
import network.customevents.player.PlayerSpectatorEvent;
import network.customevents.player.PlayerSpectatorEvent.SpectatorState;
import network.gameapi.SpectatorHandler;
import network.gameapi.games.onevsones.events.QueueEvent;
import network.gameapi.games.onevsones.kits.OneVsOneKit;
import network.player.MessageHandler;
import network.player.account.AccountHandler.Ranks;
import network.server.tasks.DelayedTask;
import network.server.util.EffectUtil;
import network.server.util.EventUtil;
import network.server.util.ItemCreator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SuppressWarnings("deprecation")
public class LobbyHandler implements Listener {
    private static String rankedQueueName = null;
    private static String unrankedQueueName = null;
    private static ItemStack rankedQueue = null;
    private static ItemStack unrankedQueue = null;
    private static List<String> disabledRequests = null;
    private static List<String> watching = null;
    private static int maxCoords = 100;

    public LobbyHandler() {
        rankedQueueName = "Ranked Queue";
        unrankedQueueName = "Unranked Queue";
        rankedQueue = new ItemCreator(Material.DIAMOND_SWORD).setName("&a" + rankedQueueName).getItemStack();
        unrankedQueue = new ItemCreator(Material.IRON_SWORD).setName("&a" + unrankedQueueName).getItemStack();
        disabledRequests = new ArrayList<>();
        watching = new ArrayList<>();
        EventUtil.register(this);
    }

    public static Location spawn(Player player) {
        return spawn(player, true);
    }

    public static Location spawn(Player player, boolean giveItems) {
        Location location = player.getWorld().getSpawnLocation();
        location.setYaw(-180f);
        location.setPitch(0.0f);
        Random random = new Random();
        int range = 3;
        location.setX(location.getBlockX() + (random.nextInt(range) * (random.nextBoolean() ? 1 : -1)));
        location.setY(location.getY() + 2.5d);
        location.setZ(location.getBlockZ() + (random.nextInt(range) * (random.nextBoolean() ? 1 : -1)));
        player.teleport(location);
        if(!SpectatorHandler.contains(player)) {
            if(Ranks.PRO.hasRank(player)) {
                player.setAllowFlight(true);
            }
            if(giveItems) {
                giveItems(player);
            }
        }
        player.setFoodLevel(20);
        Network.getSidebar().update(player);
        return location;
    }

    public static void giveItems(Player player) {
        player.getInventory().clear();
        player.getInventory().setHelmet(new ItemStack(Material.AIR));
        player.getInventory().setChestplate(new ItemStack(Material.AIR));
        player.getInventory().setLeggings(new ItemStack(Material.AIR));
        player.getInventory().setBoots(new ItemStack(Material.AIR));
        player.getInventory().setHeldItemSlot(0);
        player.getInventory().setItem(0, rankedQueue);
        player.getInventory().setItem(1, unrankedQueue);
        player.getInventory().setItem(7, HotBarEditor.getItem());
        player.getInventory().setItem(8, SpectatorHandler1v1s.getItem());
        player.updateInventory();
    }

    public static boolean isInLobby(Player player) {
        return !BattleHandler.isInBattle(player);
    }

    public static Inventory getKitSelectorInventory(Player player, String name, boolean showUsers) {
        Inventory inventory = Bukkit.createInventory(player, 9, name);

        for(OneVsOneKit kit : OneVsOneKit.getKits()) {
            if(showUsers) {
                inventory.addItem(new ItemCreator(kit.getIcon().clone()).setAmount(kit.getUsers()).getItemStack());
            } else {
                inventory.addItem(new ItemCreator(kit.getIcon().clone()).setAmount(1).getItemStack());
            }
        }

        return inventory;
    }

    public static void openKitSelection(Player player, boolean ranked) {
        player.openInventory(getKitSelectorInventory(player, ranked ? rankedQueueName : unrankedQueueName, true));
        watching.add(player.getName());
    }

    private boolean isArmor(ItemStack itemStack) {
        String name = itemStack.getType().toString();
        if(name.contains("_")) {
            name = name.split("_")[1];
            return name.matches("(HELMET|CHESTPLATE|LEGGINGS|BOOTS).*");
        }
        return false;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if(event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if(isInLobby(player)) {
                if(event.getCause() == DamageCause.VOID) {
                    spawn(player, false);
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onTime(TimeEvent event) {
        long ticks = event.getTicks();
        if(ticks == 20) {
            for(String name : watching) {
                Player player = ProPlugin.getPlayer(name);
                if(player != null) {
                    InventoryView view = player.getOpenInventory();
                    List<OneVsOneKit> kits = OneVsOneKit.getKits();
                    for(int a = 0; a < kits.size(); ++a) {
                        OneVsOneKit kit = kits.get(a);
                        ItemCreator creator = new ItemCreator(kit.getIcon().clone());
                        creator.setAmount(kit.getUsers());
                        if(creator.getAmount() % 2 != 0) {
                            creator.addEnchantment(Enchantment.DURABILITY);
                            creator.addLore("&bPlayer(s) waiting in queue");
                            creator.addLore("&bClick to play");
                            if(kit.getName().equals("UHC")) {
                                creator.setData(3);
                            }
                        }
                        view.setItem(a, creator.getItemStack());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryItemClick(InventoryItemClickEvent event) {
    	Player player = event.getPlayer();

    	// Selecting a kit
        if(event.getTitle().equals(rankedQueueName) || event.getTitle().equals(unrankedQueueName)) {
            event.setCancelled(true);
            player.closeInventory();
            OneVsOneKit kit = OneVsOneKit.getKit(event.getItem());

            if(kit == null) {
                MessageHandler.sendMessage(player, "&cAn error occurred when selecting kit, please try again");
            } else {
                QueueHandler.add(player, kit, 1, event.getTitle().equalsIgnoreCase(rankedQueueName));
                EffectUtil.playSound(player, Sound.NOTE_PLING);
            }
        }

        // Requesting to battle another player
        else if(event.getTitle().equals("Request a Battle")) {
            String name = event.getItem().getItemMeta().getDisplayName();
            new DelayedTask(() -> {
                Player player1 = ProPlugin.getPlayer(name);
                if(player1 != null) {
                    player1.chat("/battle " + name);
                }
            });
            player.closeInventory();
            event.setCancelled(true);
        }

        // Previewing a kit
//        else if(event.getTitle().startsWith("Preview of")) {
//        	Material type = event.getItem().getType();
//        	if(type == Material.WOOD_DOOR) {
//        		openKitSelection(player);
//        	}
//        	event.setCancelled(true);
//        }
        // Don't allow players to move armor in their inventory while they're in the lobby
        else if(event.getTitle().equalsIgnoreCase("container.crafting") && isArmor(event.getItem()) && isInLobby(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if(!Ranks.OWNER.hasRank(player)) {
            if(isInLobby(player) && !SpectatorHandler.contains(player)) {
                Location to = event.getTo();
                int x = to.getBlockX();
                int z = to.getBlockZ();
                if(x >= maxCoords || x <= -maxCoords || z >= maxCoords || z <= -maxCoords) {
                    event.setTo(event.getFrom());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if(isInLobby(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMouseClick(MouseClickEvent event) {
        Player player = event.getPlayer();
        if(isInLobby(player)) {
            ItemStack item = player.getItemInHand();
            if(item.equals(rankedQueue) || item.equals(unrankedQueue)) {
                openKitSelection(player, item.equals(rankedQueue));
            }
        }
    }

    @EventHandler
    public void onQueue(QueueEvent event) {
        if(event.getAction() == QueueEvent.QueueAction.REMOVE) {
            spawn(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        spawn(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(spawn(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerSpectator(PlayerSpectatorEvent event) {
    	if(event.getState() == SpectatorState.END) {
    		Player player = event.getPlayer();
            new DelayedTask(() -> spawn(player));
    	}
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if(event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            EffectUtil.playSound(player, Sound.CHEST_OPEN);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        watching.remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile projectile = (Projectile) event.getEntity();
        if(projectile.getShooter() instanceof Player) {
            Player player = (Player) projectile.getShooter();
            if(isInLobby(player)) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
    	if(isInLobby(event.getPlayer())) {
    		event.setCancelled(true);
    	}
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveEvent event) {
        Player player = event.getPlayer();
        disabledRequests.remove(player.getName());
        watching.remove(player.getName());
        OneVsOneKit.removePlayerKit(player);
    }
}
