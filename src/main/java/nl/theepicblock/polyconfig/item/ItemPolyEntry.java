package nl.theepicblock.polyconfig.item;

import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.objects.KDLValue;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;
import nl.theepicblock.polyconfig.block.ConfigFormatException;
import nl.theepicblock.polyconfig.util.ElementGroup;
import nl.theepicblock.polyconfig.util.PolyConfigEntry;
import nl.theepicblock.polyconfig.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class ItemPolyEntry extends PolyConfigEntry<Item> {
    private static final Style DEFAULT_LORE_STYLE = Style.EMPTY.withColor(Formatting.GRAY).withItalic(false);
    public static final Parser PARSER = new Parser();

    @NotNull
    private final ItemPolyProvider polyProvider;
    @NotNull
    private final List<Consumer<ItemStack>> stackModifiers;

    public ItemPolyEntry(@NotNull Item moddedElement, boolean regex, @NotNull ItemPolyProvider polyProvider, @NotNull List<Consumer<ItemStack>> stackModifiers) {
        super(moddedElement, regex);
        this.polyProvider = polyProvider;
        this.stackModifiers = stackModifiers;
    }

    public @NotNull ItemPolyProvider getPolyProvider() {
        return polyProvider;
    }

    public @NotNull List<Consumer<ItemStack>> getStackModifiers() {
        return stackModifiers;
    }

    public static class Parser extends PolyConfigEntry.Parser<Item, ItemPolyEntry> {
        public Parser() {
            super(Registry.ITEM, "item");
        }

        @Override
        public void process(@NotNull Identifier moddedId, @NotNull Item moddedElement, @NotNull KDLNode node, @NotNull Map<Identifier, ItemPolyEntry> resultMap, boolean regex) throws ConfigFormatException {
            var nodeChildren = Utils.getChildren(node);
            var replacementNode = Utils.getOptionalNode(nodeChildren, "replacement");
            if (replacementNode.getArgs().size() != 1) throw new ConfigFormatException("replacement must have exactly one argument");

            ItemPolyProvider polyProvider;
            Optional<String> replacementType = replacementNode.getType();
            String replacementString = replacementNode.getArgs().get(0).getAsString().getValue();
            if (replacementType.isEmpty() || replacementType.get().equals("item")) {
                Item replacementItem = resolveElement(replacementString);
                if (moddedElement.isDamageable() && replacementItem.isDamageable()) {
                    polyProvider = ItemPolyProvider.damageable(replacementItem);
                } else {
                    polyProvider = ItemPolyProvider.simple(replacementItem);
                }
            } else if (replacementType.get().equals("group")) {
                polyProvider = ElementGroup.getGroupByName(replacementString, ItemGroup.class, "item").polyProvider;
            } else {
                throw new ConfigFormatException("Unknown replacement type "+replacementType.get()+" for "+moddedId+", expected item or group");
            }

            List<Consumer<ItemStack>> stackModifiers = new ArrayList<>();
            KDLNode enchantedNode = Utils.getOptionalNode(nodeChildren, "enchanted");
            if (enchantedNode != null) {
                KDLValue<?> enchantedArg = Utils.getSingleArgNoProps(enchantedNode);
                if (!enchantedArg.isBoolean()) throw new ConfigFormatException("enchanted must be a boolean");
                if (enchantedArg.getAsBoolean().get().getValue()) {
                    stackModifiers.add(stack -> {
                        if (stack.hasGlint()) {
                            return;
                        }

                        NbtList enchantments = new NbtList();
                        NbtCompound dummyEnchantment = new NbtCompound();
                        dummyEnchantment.putString("id", "!dummy");
                        dummyEnchantment.putInt("lvl", -1);
                        enchantments.add(dummyEnchantment);
                        stack.getOrCreateNbt().put(ItemStack.ENCHANTMENTS_KEY, enchantments);
                    });
                }
            }

            var extraLore = new ArrayList<String>();
            for (KDLNode nodeChild : nodeChildren) {
                if (nodeChild.getIdentifier().equals("lore")) {
                    Text text = Utils.getText(Utils.getSingleArgNoProps(nodeChild), DEFAULT_LORE_STYLE);
                    extraLore.add(Text.Serializer.toJson(text));
                }
            }
            if (!extraLore.isEmpty()) {
                stackModifiers.add(stack -> {
                    NbtCompound displayCompound = stack.getOrCreateSubNbt(ItemStack.DISPLAY_KEY);
                    NbtList lore;
                    if (displayCompound.contains(ItemStack.LORE_KEY, 8)) {
                        lore = displayCompound.getList(ItemStack.LORE_KEY, 8);
                    } else {
                        lore = new NbtList();
                        displayCompound.put(ItemStack.LORE_KEY, lore);
                    }
                    for (String extra : extraLore) {
                        lore.add(NbtString.of(extra));
                    }
                });
            }

            var rarityNode = Utils.getOptionalNode(nodeChildren, "rarity");
            if (rarityNode != null) {
                var rarityArg = Utils.getSingleArgNoProps(rarityNode);
                if (!rarityArg.isString()) throw new ConfigFormatException("rarity must be a string");
                try {
                    Rarity rarity = Rarity.valueOf(rarityArg.getAsString().getValue().toUpperCase(Locale.ROOT));
                    stackModifiers.add(stack -> {
                        Text name = stack.getName();
                        TextColor color = name.getStyle().getColor();
                        if (color != null) {
                            if (Objects.equals(rarity.formatting.getColorValue(), color.getRgb())) {
                                return;
                            }
                        }

                        TextContent content = name.getContent();
                        stack.setCustomName(MutableText.of(content).styled(style -> style.withColor(rarity.formatting).withItalic(false)));
                    });
                } catch (IllegalArgumentException e) {
                    throw new ConfigFormatException("Unknown rarity "+rarityArg.getAsString().getValue());
                }
            }
            if (stackModifiers.isEmpty()) {
                stackModifiers = Collections.emptyList();
            }

            resultMap.put(moddedId, new ItemPolyEntry(moddedElement, regex, polyProvider, stackModifiers));
        }
    }
}
