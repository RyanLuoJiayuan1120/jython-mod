package net.luojiayuan.jython.mod.libs;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.python.core.PyObject;

/*
* 方块类
*
* === 如何注册方块？(Jython) ===
* from net.minecraft.world.level.block import Block
* from net.minecraft.world.level.block.state import BlockBehaviour
* from net.luojiayuan.jython.mod.libs import block
*
* === 示例1: 注册简单方块 ===
* # 创建方块属性
* props = BlockBehaviour.Properties.of()
*
* # 注册方块（带物品）
* my_block = block.register("my_block", lambda props: Block(props), props, True)
*
* === 示例2: 注册方块（不带物品） ===
* # 技术性方块，不需要物品
* tech_block = block.register("tech_block", lambda props: Block(props), props, False)
*
* === Java 原始代码示例 ===
* BlockBehaviour.Properties props = BlockBehaviour.Properties.of();
* Block block = new Block(props);
* Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK,
*     Identifier.fromNamespaceAndPath("mymod", "my_block")), block);
*/

public class block {

	/**
	 * 注册方块（Jython版本 - 接受PyObject）
	 *
	 * @param name 方块名称（不含模组ID前缀）
	 * @param ModName 模组ID
	 * @param pyCallable Python可调用对象（函数或lambda）
	 * @param settings 方块属性
	 * @param shouldRegisterItem 是否注册对应的物品
	 * @return 注册的方块实例
	 */
	public static Block register(String name, String ModName, PyObject pyCallable,
			BlockBehaviour.Properties settings, boolean shouldRegisterItem) {
		// 将Python函数转换为Java Function
		Function<BlockBehaviour.Properties, Block> blockFactory = new Function<BlockBehaviour.Properties, Block>() {
			@Override
			public Block apply(BlockBehaviour.Properties props) {
				// 调用Python函数
				PyObject result = pyCallable.__call__(org.python.core.Py.java2py(props));
				return (Block) result.__tojava__(Block.class);
			}
		};

		return register(name, ModName, blockFactory, settings, shouldRegisterItem);
	}

	/**
	 * 注册方块
	 *
	 * @param name 方块名称（不含模组ID前缀）
	 * @param blockFactory 方块工厂函数
	 * @param settings 方块属性
	 * @param shouldRegisterItem 是否注册对应的物品
	 * @return 注册的方块实例
	 */
	public static Block register(String name, String ModName, Function<BlockBehaviour.Properties, Block> blockFactory,
			BlockBehaviour.Properties settings, boolean shouldRegisterItem) {
		// Create a registry key for the block
		ResourceKey<Block> blockKey = keyOfBlock(name, ModName);
		// Create the block instance
		Block block = blockFactory.apply(settings.setId(blockKey));

		// Sometimes, you may not want to register an item for the block.
		// Eg: if it's a technical block like `minecraft:moving_piston` or `minecraft:end_gateway`
		if (shouldRegisterItem) {
			// Items need to be registered with a different type of registry key, but the ID
			// can be the same.
			ResourceKey<Item> itemKey = keyOfItem(name, ModName);

			BlockItem blockItem = new BlockItem(block,
					new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
			Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
		}

		return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
	}

	private static ResourceKey<Block> keyOfBlock(String name, String ModName) {
		return ResourceKey.create(Registries.BLOCK,
				Identifier.fromNamespaceAndPath(ModName, name));
	}

	private static ResourceKey<Item> keyOfItem(String name, String ModName) {
		return ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(ModName, name));
	}
}
