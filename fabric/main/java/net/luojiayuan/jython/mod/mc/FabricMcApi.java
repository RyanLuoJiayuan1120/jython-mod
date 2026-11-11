package net.luojiayuan.jython.mod.mc;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Built-in Fabric MC primitives for Python modules. This replaces the foreign
 * "bridge mod" that was needed before jython-mod exposed player/inventory
 * helpers: it uses official Mojmap names at compile time, so no runtime
 * mapping guesswork is required.
 *
 * <p>Python binds this as the global {@code McApi} object:</p>
 * <pre>
 * player = McApi.playerOf(ctx.getSource())
 * McApi.send(ctx.getSource(), "wallet=" + McApi.countItem(player, "minecraft:bread"))
 * </pre>
 */
public final class FabricMcApi {
    private static final Logger LOGGER = LoggerFactory.getLogger("jython-mod-mcapi");

    public FabricMcApi() {
    }

    public Object playerOf(Object source) {
        if (!(source instanceof CommandSourceStack stack)) {
            return null;
        }
        try {
            return stack.getPlayerOrException();
        } catch (Exception e) {
            return null;
        }
    }

    public String uuidOf(Object player) {
        return player instanceof ServerPlayer p ? p.getUUID().toString() : "";
    }

    public String nameOf(Object player) {
        return player instanceof ServerPlayer p ? p.getName().getString() : "";
    }

    public long seedOf(Object server) {
        return server instanceof MinecraftServer s ? s.overworld().getSeed() : 0L;
    }

    public boolean isOp(Object source) {
        if (source instanceof CommandSourceStack stack) {
            return stack.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);
        }
        return true;
    }

    public String heldItemId(Object player) {
        if (!(player instanceof ServerPlayer p)) {
            return "";
        }
        return itemId(p.getMainHandItem());
    }

    public String itemId(Object stackObject) {
        if (!(stackObject instanceof ItemStack stack) || stack.isEmpty()) {
            return "";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    public int countItem(Object player, String itemId) {
        if (!(player instanceof ServerPlayer p)) {
            return 0;
        }
        Inventory inv = p.getInventory();
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && itemId.equals(itemId(stack))) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public int removeItem(Object player, String itemId, int max) {
        if (!(player instanceof ServerPlayer p) || max <= 0) {
            return 0;
        }
        Inventory inv = p.getInventory();
        int remaining = max;
        int removed = 0;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || !itemId.equals(itemId(stack))) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
            removed += take;
        }
        return removed;
    }

    public int giveItem(Object player, String itemId, int count) {
        if (!(player instanceof ServerPlayer p) || count <= 0) {
            return 0;
        }
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) {
            return 0;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == null || !id.equals(BuiltInRegistries.ITEM.getKey(item))) {
            return 0;
        }
        ItemStack stack = new ItemStack(item, count);
        int before = stack.getCount();
        p.getInventory().add(stack);
        return before - stack.getCount();
    }

    public void send(Object source, String text) {
        if (source instanceof CommandSourceStack stack) {
            stack.sendSuccess(() -> Component.literal(text), false);
        } else {
            LOGGER.info(text);
        }
    }

    public void sendFailure(Object source, String text) {
        if (source instanceof CommandSourceStack stack) {
            stack.sendFailure(Component.literal(text));
        } else {
            LOGGER.warn(text);
        }
    }

    public int onlinePlayers(Object server) {
        return server instanceof MinecraftServer s ? s.getPlayerList().getPlayerCount() : 0;
    }
}
