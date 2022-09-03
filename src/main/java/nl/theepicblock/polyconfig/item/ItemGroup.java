package nl.theepicblock.polyconfig.item;

import io.github.theepicblock.polymc.api.item.CustomModelDataManager;
import io.github.theepicblock.polymc.api.item.ItemPoly;
import io.github.theepicblock.polymc.impl.poly.item.CustomModelDataPoly;
import io.github.theepicblock.polymc.impl.poly.item.DamageableItemPoly;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import nl.theepicblock.polyconfig.util.ElementGroup;

import java.util.function.BiFunction;

public enum ItemGroup implements ElementGroup {
    DEFAULT("default", wrapCmdPolyFunction(CustomModelDataPoly::new)),
    FUELS("fuels", ItemPolyProvider.simple(CustomModelDataManager.FUEL_ITEMS)),
    DYEABLES("dyeables", ItemPolyProvider.simple(Items.LEATHER_HORSE_ARMOR)),
    FOODS("foods", ItemPolyProvider.simple(CustomModelDataManager.FOOD_ITEMS)),
    DAMAGEABLES("damageables", wrapCmdPolyFunction(DamageableItemPoly::new)),
    DYEABLE_DAMAGEABLES("dyeable_damageables", ItemPolyProvider.damageable(Items.LEATHER_HORSE_ARMOR));

    public final String name;
    public final ItemPolyProvider polyProvider;

    ItemGroup(String name, ItemPolyProvider polyProvider) {
        this.name = name;
        this.polyProvider = polyProvider;
    }

    @Override
    public String getName() {
        return name;
    }

    private static ItemPolyProvider wrapCmdPolyFunction(BiFunction<CustomModelDataManager, Item, ItemPoly> polyFunction) {
        return (registry, item) -> polyFunction.apply(registry.getSharedValues(CustomModelDataManager.KEY), item);
    }
}
