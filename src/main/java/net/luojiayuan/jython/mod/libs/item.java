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
* 如何注册？(Jython)
* from net.minecraft.world.item import Item
* from net.luojiayuan.jython.mod.libs import item
* xxx = item.register("物品名",<模组名>, lambda: Item(), Item.Properties());
*
* === 毒食物示例 (Java) ===
* public static final Consumable POISON_FOOD_CONSUMABLE_COMPONENT = Consumables.defaultFood()
*         // The duration is in ticks, 20 ticks = 1 second
*         .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.POISON, 6 * 20, 1), 1.0f))
*         .build();
*
* public static final FoodProperties POISON_FOOD_COMPONENT = new FoodProperties.Builder()
*         .alwaysEdible()
*         .build();
*
* === 毒食物示例 (Jython) ===
* from net.minecraft.world.effect import MobEffects
* from net.minecraft.world.effect import MobEffectInstance
* from net.minecraft.world.food import FoodProperties
* from net.minecraft.world.food import FoodConsumable
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
	public static <T extends Item> T register(String name, String ModName, Function<Item.Properties, T> itemFactory, Item.Properties settings) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ModName, name));
		T item = itemFactory.apply(settings.setId(itemKey));
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);

		return item;
	}

}