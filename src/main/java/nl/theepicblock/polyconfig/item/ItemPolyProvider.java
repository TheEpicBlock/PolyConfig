package nl.theepicblock.polyconfig.item;

import io.github.theepicblock.polymc.api.PolyRegistry;
import io.github.theepicblock.polymc.api.item.CustomModelDataManager;
import io.github.theepicblock.polymc.api.item.ItemPoly;
import io.github.theepicblock.polymc.impl.generator.ItemPolyGenerator;
import io.github.theepicblock.polymc.impl.poly.item.CustomModelDataPoly;
import io.github.theepicblock.polymc.impl.poly.item.DamageableItemPoly;
import net.minecraft.item.Item;

@FunctionalInterface
public interface ItemPolyProvider {
    ItemPolyProvider AUTO = (item, builder) -> ItemPolyGenerator.generatePoly(builder, item);
    static ItemPolyProvider simple(Item... items) {
        return (registry, moddedItem) -> new CustomModelDataPoly(registry.getSharedValues(CustomModelDataManager.KEY), moddedItem, items);
    }
    static ItemPolyProvider damageable(Item... items) {
        return (registry, moddedItem) -> new DamageableItemPoly(registry.getSharedValues(CustomModelDataManager.KEY), moddedItem, items);
    }

    ItemPoly create(PolyRegistry registry, Item moddedItem);
}
