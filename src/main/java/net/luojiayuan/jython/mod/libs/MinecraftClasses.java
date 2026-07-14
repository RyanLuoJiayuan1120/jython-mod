package net.luojiayuan.jython.mod.libs;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

/**
 * Minecraft 类的辅助类，用于 Python 访问
 *
 * 使用示例:
 * from net.luojiayuan.jython.mod.libs import MinecraftClasses as mc
 *
 * # 创建方块属性
 * props = mc.Block_Properties.of()
 *
 * # 创建方块
 * block = mc.Block(props)
 *
 * # 创建物品属性
 * item_props = mc.Item_Properties()
 *
 * # 创建物品
 * item = mc.Item(item_props)
 */
public class MinecraftClasses {
	// 方块类
	public static Class<Block> Block = Block.class;
	public static Class<BlockBehaviour.Properties> Block_Properties = BlockBehaviour.Properties.class;

	// 物品类
	public static Class<Item> Item = Item.class;
	public static Class<Item.Properties> Item_Properties = Item.Properties.class;
	public static Class<BlockItem> BlockItem = BlockItem.class;

	/**
	 * 获取方块属性的静态方法
	 */
	public static BlockBehaviour.Properties createBlockProperties() {
		return BlockBehaviour.Properties.of();
	}

	/**
	 * 创建方块实例
	 */
	public static Block createBlock(BlockBehaviour.Properties props) {
		return new Block(props);
	}

	/**
	 * 从道具属性创建方块（用于 lambda）
	 */
	public static Block createBlockFromProps(Object props) {
		return new Block((BlockBehaviour.Properties) props);
	}

	/**
	 * 获取物品属性的静态方法
	 */
	public static Item.Properties createItemProperties() {
		return new Item.Properties();
	}

	/**
	 * 获取物品属性的静态方法（别名，与 Item.Properties 保持一致）
	 */
	public static Item.Properties createItemSettings() {
		return new Item.Properties();
	}

	/**
	 * 创建物品实例
	 */
	public static Item createItem(Item.Properties props) {
		return new Item(props);
	}

	/**
	 * 从道具属性创建物品（用于 lambda）
	 */
	public static Item createItemFromProps(Object props) {
		return new Item((Item.Properties) props);
	}

	/**
	 * 创建方块物品实例
	 */
	public static BlockItem createBlockItem(Block block, Item.Properties props) {
		return new BlockItem(block, props);
	}
}
