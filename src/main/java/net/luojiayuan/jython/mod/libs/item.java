package net.luojiayuan.jython.mod.libs;

import java.util.function.Function;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import org.python.core.PyObject;

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
	/**
	 * 注册物品（Jython版本 - 接受PyObject）
	 *
	 * @param name 物品名称（不含模组ID前缀）
	 * @param ModName 模组ID
	 * @param pyCallable Python可调用对象（函数或lambda）
	 * @param settings 物品属性
	 * @return 注册的物品实例
	 */
	public static Item register(String name, String ModName, PyObject pyCallable, Item.Properties settings) {
		// 将Python函数转换为Java Function
		Function<Item.Properties, Item> itemFactory = new Function<Item.Properties, Item>() {
			@Override
			public Item apply(Item.Properties props) {
				// 调用Python函数
				PyObject result = pyCallable.__call__(org.python.core.Py.java2py(props));
				return (Item) result.__tojava__(Item.class);
			}
		};

		return register(name, ModName, itemFactory, settings);
	}

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