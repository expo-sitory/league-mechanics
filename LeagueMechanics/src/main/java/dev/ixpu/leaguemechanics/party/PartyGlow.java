package dev.ixpu.leaguemechanics.party;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import dev.ixpu.leaguemechanics.LeagueMechanics;
import dev.ixpu.leaguemechanics.manager.PartyManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class PartyGlow {
    private static final byte FLAG_ON_FIRE = 0x01;
    private static final byte FLAG_CROUCHING = 0x02;
    private static final byte FLAG_SPRINTING = 0x08;
    private static final byte FLAG_SWIMMING = 0x10;
    private static final byte FLAG_INVISIBLE = 0x20;
    private static final byte FLAG_GLOWING = 0x40;
    private static final byte FLAG_GLIDING = (byte) 0x80;

    private final ProtocolManager protocol;

    public PartyGlow(LeagueMechanics plugin, PartyManager parties) {
        this.protocol = ProtocolLibrary.getProtocolManager();

        protocol.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player viewer = event.getPlayer();
                if (viewer == null) return;
                int entityId = event.getPacket().getIntegers().read(0);
                if (!parties.isPartyMateEntity(viewer, entityId)) return;
                if (parties.isGlowSuppressed(viewer)) return;

                PacketContainer packet = event.getPacket().deepClone();
                List<WrappedDataValue> values = packet.getDataValueCollectionModifier().read(0);
                boolean changed = false;
                for (WrappedDataValue value : values) {
                    if (value.getIndex() == 0 && value.getValue() instanceof Byte flags) {
                        value.setValue((byte) (flags | FLAG_GLOWING));
                        changed = true;
                    }
                }
                if (changed) {
                    packet.getDataValueCollectionModifier().write(0, values);
                    event.setPacket(packet);
                }
            }
        });
    }

    public void refresh(Player viewer, Player target) {
        if (!viewer.isOnline() || !target.isOnline()) return;

        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());

        List<WrappedDataValue> values = new ArrayList<>();
        values.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), entityFlags(target)));
        packet.getDataValueCollectionModifier().write(0, values);

        protocol.sendServerPacket(viewer, packet);
    }

    private byte entityFlags(Player target) {
        byte flags = 0;
        if (target.getFireTicks() > 0) flags |= FLAG_ON_FIRE;
        if (target.isSneaking()) flags |= FLAG_CROUCHING;
        if (target.isSprinting()) flags |= FLAG_SPRINTING;
        if (target.isSwimming()) flags |= FLAG_SWIMMING;
        if (target.isInvisible()) flags |= FLAG_INVISIBLE;
        if (target.isGlowing()) flags |= FLAG_GLOWING;
        if (target.isGliding()) flags |= FLAG_GLIDING;
        return flags;
    }
}