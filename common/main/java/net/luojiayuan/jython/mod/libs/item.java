package net.luojiayuan.jython.mod.libs;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/*
* 物品类
*
* === 基础注册示例 ===
* 如何注册？
* from net.minecraft.world.item import Item
* from net.luojiayuan.jython.mod.libs import item
* xxx = item.register("物品名",<模组名>, lambda: Item(), Item.Properties());
*
* === 毒食物示例 ===
* from net.minecraft.world.effect import MobEffects
* from net.minecraft.world.effect import MobEffectInstance
* from net.minecraft.world.food import FoodProperties
* from net.minecraft.world.item.consumables import Consumable
* from net.minecraft.world.item.consumables import Consumables
* from net.minecraft.world.item.consumponents import ApplyStatusEffectsConsumeEffect
*
* # 创建毒效果消耗品组件 (持续6秒，中毒效果I级)
* poison_effect = MobEffectInstance(MobEffects.POISON, 6 * 20, 1)
* poison_consume_effect = ApplyStatusEffectsConsumeEffect(poison_effect, 1.0)
* POISON_FOOD_CONSUMABLE_COMPONENT = Consumables.defaultFood().onConsume(poison_consume_effect).build()
*
* # 创建食物属性 (始终可食用)
* POISON_FOOD_COMPONENT = FoodProperties.Builder().alwaysEdible().build()
*
* # 注册毒食物
* from net.minecraft.world.item import Item
* poison_food = item.register("poison_food", "mymod", lambda: Item(POISON_FOOD_COMPONENT), Item.Properties().food(POISON_FOOD_COMPONENT))
*/


public class item {
	/**
	 * 注册物品
	 *
	 * @param name 物品名称（不含模组ID前缀）
	 * @param ModName 模组ID
	 * @param itemFactory 物品工厂函数
	 * @param settings 物品属性
	 * @return 注册的物品实例
	 */
	public static <T extends Item> T register(String name, String ModName, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ModName, name));

		// Check if item is already registered
		if (BuiltInRegistries.ITEM.containsKey(itemKey)) {
			// Item already exists, try to get it from registry
			try {
				var holder = BuiltInRegistries.ITEM.get(itemKey);
				if (holder.isPresent()) {
					return (T) holder.get().value();
				}
			} catch (Exception e) {
				// Fall through to create new item
			}
		}

		T item = itemFactory.apply(settings.setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return item;
	}

}
