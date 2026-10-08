package dev.ixpu.leaguemechanics.manager;

import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.entity.player.PlayerClass;
import dev.ixpu.leaguemechanics.entity.player.PlayerClassType;
import dev.ixpu.leaguemechanics.entity.player.PlayerStats;
import dev.ixpu.leaguemechanics.party.PartyGlow;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PartyManager implements Listener {
    public static final double INVITE_RANGE = 20.0;
    public static final long INVITE_TTL_TICKS = 20L * 20L;
    private static final long GLOW_REFRESH_TICKS = 40L;

    private static PartyManager instance;

    private static final class Party {
        final UUID id;
        final Set<UUID> members = ConcurrentHashMap.newKeySet();
        volatile UUID owner;

        Party(UUID id) {
            this.id = id;
        }
    }

    private static final class Invite {
        final UUID inviter;
        final String inviterName;

        Invite(UUID inviter, String inviterName) {
            this.inviter = inviter;
            this.inviterName = inviterName;
        }
    }

    private final Map<UUID, Party> partyByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Party> partiesById = new ConcurrentHashMap<>();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "LeagueMechanics-Party-DB");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, LinkedList<Invite>> invites = new ConcurrentHashMap<>();

    private PartyGlow glow;

    private PartyManager() {
    }

    public static PartyManager getInstance() {
        if (instance == null) {
            instance = new PartyManager();
        }
        return instance;
    }

    public void enable(LeagueMechanics plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);

        for (Player online : Bukkit.getOnlinePlayers()) {
            loadParty(online);
        }

        if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            try {
                glow = new PartyGlow(plugin, this);
                Bukkit.getScheduler().runTaskTimer(plugin, this::refreshAllGlow, GLOW_REFRESH_TICKS, GLOW_REFRESH_TICKS);
                plugin.getLogger().info("Party glow enabled (ProtocolLib found).");
            } catch (Throwable t) {
                glow = null;
                plugin.getLogger().warning("Party glow disabled, ProtocolLib hook failed: " + t.getMessage());
            }
        } else {
            plugin.getLogger().warning("ProtocolLib not found: parties work, but members will not glow.");
        }
    }


    public void disable() {
        dbExecutor.shutdown();
        try {
            dbExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void loadParty(Player player) {
        UUID uuid = player.getUniqueId();
        LeagueMechanics plugin = LeagueMechanics.getInstance();
        MySQLManager db = plugin.getMySQLManager();
        if (db == null) return;
        dbExecutor.execute(() -> {
            UUID partyId = db.loadPartyId(uuid);
            if (partyId == null) return;
            Set<UUID> members = db.loadPartyMembers(partyId);
            UUID owner = db.loadPartyOwner(partyId);
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> attach(uuid, partyId, members, owner));
        });
    }

    private void attach(UUID uuid, UUID partyId, Set<UUID> members, UUID owner) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline() || partyByPlayer.containsKey(uuid)) return;

        Party party = partiesById.get(partyId);
        if (party == null) {
            party = new Party(partyId);
            party.members.addAll(members);
            partiesById.put(partyId, party);
        }
        if (party.owner == null) {
            party.owner = owner;
        }
        party.members.add(uuid);
        partyByPlayer.put(uuid, party);

        for (Player member : onlineMembers(party)) {
            if (member != player) glowPair(member, player);
        }
    }

    private void persist(Runnable task) {
        if (LeagueMechanics.getInstance().getMySQLManager() == null) return;
        dbExecutor.execute(task);
    }

    private void persistOwner(UUID partyId, UUID owner) {
        MySQLManager db = LeagueMechanics.getInstance().getMySQLManager();
        persist(() -> db.savePartyOwner(partyId, owner));
    }

    private void persistMember(UUID partyId, UUID player) {
        MySQLManager db = LeagueMechanics.getInstance().getMySQLManager();
        persist(() -> db.savePartyMember(partyId, player));
    }

    private static final int UNIQUE_CLASS_LIMIT = 1;
    private static final int DEFAULT_CLASS_LIMIT = 2;

    private int classLimit(PlayerClassType type) {
        return type == PlayerClassType.TANK || type == PlayerClassType.SUPPORT ? UNIQUE_CLASS_LIMIT : DEFAULT_CLASS_LIMIT;
    }

    public String describeClassLimit(PlayerClassType type) {
        int limit = classLimit(type);
        return limit == 1 ? "a " + type.getDisplayName() : limit + " " + type.getDisplayName() + "s";
    }

    private PlayerClassType classOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return PlayerClass.getPlayerClass(online);
        return LeagueMechanics.getInstance().getRunePersistence().loadPlayerClass(uuid);
    }

    private boolean isGroupFull(Set<UUID> group, UUID exclude, PlayerClassType type) {
        int count = 0;
        for (UUID uuid : group) {
            if (uuid.equals(exclude)) continue;
            if (classOf(uuid) == type) count++;
        }
        return count >= classLimit(type);
    }

    private PlayerClassType findClassConflict(Player inviter, Player joiner) {
        PlayerClassType type = PlayerClass.getPlayerClass(joiner);
        if (type == null) return null;

        Party party = partyByPlayer.get(inviter.getUniqueId());
        Set<UUID> group = party != null ? party.members : Set.of(inviter.getUniqueId());
        return isGroupFull(group, joiner.getUniqueId(), type) ? type : null;
    }

    public boolean isClassTakenInParty(Player player, PlayerClassType type) {
        if (type == null) return false;
        Party party = partyByPlayer.get(player.getUniqueId());
        if (party == null) return false;
        return isGroupFull(party.members, player.getUniqueId(), type);
    }

    private NamespacedKey invitesDisabledKey() {
        return new NamespacedKey(LeagueMechanics.getInstance(), "party_invites_disabled");
    }

    public boolean areInvitesDisabled(Player player) {
        Byte value = player.getPersistentDataContainer().get(invitesDisabledKey(), PersistentDataType.BYTE);
        return value != null && value == 1;
    }

    public void toggleInvites(Player player) {
        boolean disable = !areInvitesDisabled(player);
        if (disable) {
            player.getPersistentDataContainer().set(invitesDisabledKey(), PersistentDataType.BYTE, (byte) 1);
            invites.remove(player.getUniqueId());
            player.sendMessage(Component.text("§cParty invitations disabled."));
        } else {
            player.getPersistentDataContainer().remove(invitesDisabledKey());
            player.sendMessage(Component.text("§aParty invitations enabled."));
        }
    }

    public boolean isInParty(Player player) {
        return player != null && partyByPlayer.containsKey(player.getUniqueId());
    }

    public boolean isLeader(Player player) {
        Party party = player == null ? null : partyByPlayer.get(player.getUniqueId());
        return party != null && player.getUniqueId().equals(party.owner);
    }

    public String getLeaderName(Player player) {
        Party party = player == null ? null : partyByPlayer.get(player.getUniqueId());
        if (party == null || party.owner == null) return "";
        return nameOf(party.owner);
    }

    public String getInviterName(Player player) {
        LinkedList<Invite> queue = player == null ? null : invites.get(player.getUniqueId());
        if (queue == null) return "";
        for (Invite invite : queue) {
            Player inviter = Bukkit.getPlayer(invite.inviter);
            if (inviter != null && inviter.isOnline()) return invite.inviterName;
        }
        return "";
    }

    public String getMemberName(Player player, int index) {
        UUID uuid = getMemberUuid(player, index);
        return uuid == null ? "" : nameOf(uuid);
    }

    private UUID getPartyMemberUuid(Player player, int index) {
        Party party = player == null ? null : partyByPlayer.get(player.getUniqueId());
        if (party == null || index < 0) return null;
        if (index == 0) return party.owner != null && !nameOf(party.owner).isEmpty() ? party.owner : null;

        List<UUID> uuids = new ArrayList<>();
        for (UUID uuid : party.members) {
            if (uuid.equals(party.owner)) continue;
            if (!nameOf(uuid).isEmpty()) uuids.add(uuid);
        }
        uuids.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(nameOf(a), nameOf(b)));
        return index <= uuids.size() ? uuids.get(index - 1) : null;
    }

    public String getPartyMemberName(Player player, int index) {
        UUID uuid = getPartyMemberUuid(player, index);
        return uuid == null ? "" : nameOf(uuid);
    }

    public PlayerClassType getPartyMemberClass(Player player, int index) {
        UUID uuid = getPartyMemberUuid(player, index);
        return uuid == null ? null : classOf(uuid);
    }

    public int getPartyMemberLevel(Player player, int index) {
        UUID uuid = getPartyMemberUuid(player, index);
        if (uuid == null) return 0;
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return PlayerStats.getOrCreate(online).getLeagueLevel();
        return LeagueMechanics.getInstance().getRunePersistence().loadLeagueLevel(uuid);
    }

    public Player getMemberPlayer(Player player, int index) {
        UUID uuid = getMemberUuid(player, index);
        return uuid == null ? null : Bukkit.getPlayer(uuid);
    }

    private UUID getMemberUuid(Player player, int index) {
        Party party = player == null ? null : partyByPlayer.get(player.getUniqueId());
        if (party == null || index < 1) return null;
        List<UUID> uuids = new ArrayList<>();
        for (UUID uuid : party.members) {
            if (uuid.equals(player.getUniqueId())) continue;
            if (!nameOf(uuid).isEmpty()) uuids.add(uuid);
        }
        uuids.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(nameOf(a), nameOf(b)));
        return index <= uuids.size() ? uuids.get(index - 1) : null;
    }

    private String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) return online.getName();
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "" : name;
    }

    public List<Player> getOnlineTeammates(Player player) {
        Party party = player == null ? null : partyByPlayer.get(player.getUniqueId());
        if (party == null) return new ArrayList<>();
        List<Player> teammates = onlineMembers(party);
        teammates.removeIf(member -> member.getUniqueId().equals(player.getUniqueId()));
        return teammates;
    }

    public boolean isSameParty(Entity a, Entity b) {
        if (!(a instanceof Player pa) || !(b instanceof Player pb)) return false;
        Party party = partyByPlayer.get(pa.getUniqueId());
        return party != null && party == partyByPlayer.get(pb.getUniqueId());
    }

    public boolean isPartyMateEntity(Player viewer, int entityId) {
        Party party = partyByPlayer.get(viewer.getUniqueId());
        if (party == null) return false;
        for (UUID uuid : party.members) {
            if (uuid.equals(viewer.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(uuid);
            if (member != null && member.getEntityId() == entityId) return true;
        }
        return false;
    }

    public void invite(Player inviter, Player target) {
        if (target == null || target.equals(inviter)) {
            inviter.sendMessage(Component.text("§cYou can't invite yourself."));
            return;
        }
        if (isSameParty(inviter, target)) {
            inviter.sendMessage(Component.text("§e" + target.getName() + " §cis already in your party."));
            return;
        }
        if (isInParty(target)) {
            inviter.sendMessage(Component.text("§e" + target.getName() + " §cis already in a party."));
            return;
        }
        if (areInvitesDisabled(target)) {
            inviter.sendMessage(Component.text("§e" + target.getName() + " §cis not accepting party invitations."));
            return;
        }
        PlayerClassType conflict = findClassConflict(inviter, target);
        if (conflict != null) {
            inviter.sendMessage(Component.text("§cCan't invite §e" + target.getName() + "§c: your party already has §e" + describeClassLimit(conflict) + "§c."));
            return;
        }
        if (!inviter.getWorld().equals(target.getWorld())
                || inviter.getLocation().distanceSquared(target.getLocation()) > INVITE_RANGE * INVITE_RANGE) {
            inviter.sendMessage(Component.text("§e" + target.getName() + " §cmust be within §e" + (int) INVITE_RANGE + " blocks §cto be invited."));
            return;
        }

        LinkedList<Invite> queue = invites.computeIfAbsent(target.getUniqueId(), k -> new LinkedList<>());
        queue.removeIf(i -> i.inviter.equals(inviter.getUniqueId()));
        Invite invite = new Invite(inviter.getUniqueId(), inviter.getName());
        queue.add(invite);

        long seconds = INVITE_TTL_TICKS / 20L;
        inviter.sendMessage(Component.text("§aParty invite sent to §e" + target.getName() + " §7(expires in " + seconds + "s)"));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "dwopen party-invite-menu " + target.getName());

        Bukkit.getScheduler().runTaskLater(LeagueMechanics.getInstance(), () -> expire(target.getUniqueId(), invite), INVITE_TTL_TICKS);
    }

    public void accept(Player player) {
        if (isInParty(player)) {
            player.sendMessage(Component.text("§cYou're already in a party. Use §e/party leave §cfirst."));
            return;
        }

        LinkedList<Invite> queue = invites.get(player.getUniqueId());
        Player inviter = null;
        while (queue != null && !queue.isEmpty()) {
            Invite next = queue.pollFirst();
            Player candidate = Bukkit.getPlayer(next.inviter);
            if (candidate != null && candidate.isOnline()) {
                inviter = candidate;
                break;
            }
        }
        if (inviter == null) {
            player.sendMessage(Component.text("§cYou have no pending party invites."));
            return;
        }

        PlayerClassType conflict = findClassConflict(inviter, player);
        if (conflict != null) {
            player.sendMessage(Component.text("§cYou can't join §e" + inviter.getName() + "§c's party: it already has §e" + describeClassLimit(conflict) + "§c."));
            inviter.sendMessage(Component.text("§e" + player.getName() + " §ccan't join: your party already has §e" + describeClassLimit(conflict) + "§c."));
            return;
        }
        invites.remove(player.getUniqueId());

        Party party = partyByPlayer.get(inviter.getUniqueId());
        if (party == null) {
            party = new Party(UUID.randomUUID());
            party.owner = inviter.getUniqueId();
            party.members.add(inviter.getUniqueId());
            partyByPlayer.put(inviter.getUniqueId(), party);
            partiesById.put(party.id, party);
            persistMember(party.id, inviter.getUniqueId());
            persistOwner(party.id, inviter.getUniqueId());
        }

        List<Player> existing = onlineMembers(party);
        party.members.add(player.getUniqueId());
        partyByPlayer.put(player.getUniqueId(), party);
        persistMember(party.id, player.getUniqueId());

        for (Player member : existing) {
            member.sendMessage(Component.text("§e" + player.getName() + " §ajoined the party."));
        }
        player.sendMessage(Component.text("§aYou joined §e" + inviter.getName() + "§a's party."));

        for (Player member : existing) {
            glowPair(member, player);
        }
    }

    public void leave(Player player) {
        if (!isInParty(player)) {
            player.sendMessage(Component.text("§cYou're not in a party."));
            return;
        }
        Party party = partyByPlayer.get(player.getUniqueId());
        if (party != null && player.getUniqueId().equals(party.owner)) {
            disbandParty(party, "§cThe party was disbanded because the owner §e" + player.getName() + " §cleft.");
            return;
        }
        removeFromParty(player, true);
        player.sendMessage(Component.text("§aYou left the party."));
    }

    public void disband(Player player) {
        Party party = partyByPlayer.get(player.getUniqueId());
        if (party == null) {
            player.sendMessage(Component.text("§cYou're not in a party."));
            return;
        }
        if (party.owner != null && !party.owner.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("§cOnly the party owner can disband the party."));
            return;
        }
        disbandParty(party, "§cThe party was disbanded by §e" + player.getName() + "§c.");
    }

    private void disbandParty(Party party, String message) {
        List<Player> online = onlineMembers(party);
        for (UUID uuid : new ArrayList<>(party.members)) {
            partyByPlayer.remove(uuid);
        }
        party.members.clear();
        partiesById.remove(party.id);
        MySQLManager db = LeagueMechanics.getInstance().getMySQLManager();
        persist(() -> db.deleteParty(party.id));

        for (int i = 0; i < online.size(); i++) {
            online.get(i).sendMessage(Component.text(message));
            for (int j = i + 1; j < online.size(); j++) {
                glowPair(online.get(i), online.get(j));
            }
        }
    }


    private void expire(UUID inviteeId, Invite invite) {
        LinkedList<Invite> queue = invites.get(inviteeId);
        if (queue == null || !queue.remove(invite)) return;
        if (queue.isEmpty()) invites.remove(inviteeId);

        Player invitee = Bukkit.getPlayer(inviteeId);
        Player inviter = Bukkit.getPlayer(invite.inviter);
        if (invitee != null) {
            invitee.sendMessage(Component.text("§cThe party invite from §e" + invite.inviterName + " §cexpired."));
        }
        if (inviter != null) {
            inviter.sendMessage(Component.text("§cYour party invite to §e" + (invitee != null ? invitee.getName() : "that player") + " §cexpired."));
        }
    }

    private void removeFromParty(Player player, boolean notify) {
        Party party = partyByPlayer.remove(player.getUniqueId());
        if (party == null) return;
        party.members.remove(player.getUniqueId());
        MySQLManager db = LeagueMechanics.getInstance().getMySQLManager();
        persist(() -> db.deletePartyMember(player.getUniqueId()));

        List<Player> remaining = onlineMembers(party);
        for (Player member : remaining) {
            glowPair(member, player);
            if (notify) member.sendMessage(Component.text("§e" + player.getName() + " §cleft the party."));
        }

        if (party.members.size() <= 1) {
            for (UUID uuid : new ArrayList<>(party.members)) {
                partyByPlayer.remove(uuid);
            }
            party.members.clear();
            partiesById.remove(party.id);
            persist(() -> db.deleteParty(party.id));
            if (notify) {
                for (Player member : remaining) {
                    member.sendMessage(Component.text("§cThe party was disbanded."));
                }
            }
        }
    }

    private List<Player> onlineMembers(Party party) {
        List<Player> online = new ArrayList<>();
        for (UUID uuid : party.members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) online.add(p);
        }
        return online;
    }

    private void glowPair(Player a, Player b) {
        if (glow == null) return;
        glow.refresh(a, b);
        glow.refresh(b, a);
    }

    private void refreshAllGlow() {
        if (glow == null || partyByPlayer.isEmpty()) return;
        Set<Party> seen = new HashSet<>();
        for (Party party : partyByPlayer.values()) {
            if (!seen.add(party)) continue;
            List<Player> online = onlineMembers(party);
            for (Player viewer : online) {
                for (Player target : online) {
                    if (viewer != target && viewer.getWorld().equals(target.getWorld())) {
                        glow.refresh(viewer, target);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        invites.remove(player.getUniqueId());
        for (Iterator<LinkedList<Invite>> it = invites.values().iterator(); it.hasNext(); ) {
            LinkedList<Invite> queue = it.next();
            queue.removeIf(i -> i.inviter.equals(player.getUniqueId()));
            if (queue.isEmpty()) it.remove();
        }
        unloadParty(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadParty(event.getPlayer());
    }

    private void unloadParty(Player player) {
        Party party = partyByPlayer.remove(player.getUniqueId());
        if (party == null) return;

        List<Player> remaining = onlineMembers(party);
        remaining.removeIf(member -> member.getUniqueId().equals(player.getUniqueId()));
        for (Player member : remaining) {
            glowPair(member, player);
        }
        if (remaining.isEmpty()) {
            partiesById.remove(party.id);
        }
    }
}