package me.rages.reliableframework.utils;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

public class InventoryUtil {

    public static String toBase64(Inventory inventory) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(inventory.getSize());

            for (int i = 0; i < inventory.getSize(); i++) {
                dataOutput.writeObject(inventory.getItem(i));
            }

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize item stacks!", e);
        }
    }

    public static ItemStack itemFromBase64(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            
            return (ItemStack) dataInput.readObject();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize item stack!", e);
        }
    }

    public static String itemToBase64(ItemStack itemStack) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(itemStack);

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize item stack!", e);
        }
    }

    public static ItemStack[] itemArrayFromBase64(String data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            int size = dataInput.readInt();
            ItemStack[] items = new ItemStack[size];

            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            return items;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize item stack array!", e);
        }
    }

    public static String itemArrayToBase64(ItemStack[] items) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialize item stack array!", e);
        }
    }

    public static boolean isCustomNameMatch(ItemStack stack1, ItemStack stack2) {
        String customName1 = stack1.getItemMeta() != null ? stack1.getItemMeta().getDisplayName() : null;
        String customName2 = stack2.getItemMeta() != null ? stack2.getItemMeta().getDisplayName() : null;

        return customName1 != null && customName1.equals(customName2);
    }

    public static boolean removeItem(Player player, ItemStack itemStack, int amount) {
        Map<Integer, ? extends ItemStack> items = player.getInventory().all(itemStack.getType());
        int found = items.values().stream()
                .filter(stack -> isCustomNameMatch(stack, itemStack))
                .mapToInt(ItemStack::getAmount)
                .sum();

        if (amount > found) {
            return false;
        }

        int remaining = amount;
        for (Map.Entry<Integer, ? extends ItemStack> entry : items.entrySet()) {
            ItemStack stack = entry.getValue();
            if (!isCustomNameMatch(stack, itemStack)) {
                continue;
            }

            int toRemove = Math.min(remaining, stack.getAmount());
            remaining -= toRemove;

            if (stack.getAmount() == toRemove) {
                player.getInventory().setItem(entry.getKey(), null);
            } else {
                stack.setAmount(stack.getAmount() - toRemove);
            }

            if (remaining <= 0) {
                break;
            }
        }

        player.updateInventory();
        return true;
    }

    public static Inventory fromBase64(String data, String title) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            int size = dataInput.readInt();
            Inventory inventory = Bukkit.getServer().createInventory(null, size, title);

            for (int i = 0; i < size; i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }

            return inventory;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize inventory!", e);
        }
    }

}
