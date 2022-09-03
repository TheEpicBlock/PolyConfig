package nl.theepicblock.polyconfig.block;

import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.objects.KDLValue;
import io.github.theepicblock.polymc.api.block.BlockStateMerger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import nl.theepicblock.polyconfig.util.PolyConfigEntry;
import nl.theepicblock.polyconfig.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockPolyEntry extends PolyConfigEntry<Block> {
    public static final Parser PARSER = new Parser();

    @NotNull
    private final BlockStateMerger stateMerger;
    @NotNull
    private final BlockStateSubgroup stateSubgroup;

    public BlockPolyEntry(@NotNull Block moddedElement, boolean regex, @NotNull BlockStateMerger stateMerger, @NotNull BlockStateSubgroup stateSubgroup) {
        super(moddedElement, regex);
        this.stateMerger = stateMerger;
        this.stateSubgroup = stateSubgroup;
    }

    public @NotNull BlockStateMerger getStateMerger() {
        return stateMerger;
    }

    public @NotNull BlockStateSubgroup getStateSubgroup() {
        return stateSubgroup;
    }

    public static class Parser extends PolyConfigEntry.Parser<Block, BlockPolyEntry> {
        protected Parser() {
            super(Registry.BLOCK, "block");
        }

        @Override
        public void process(@NotNull Identifier moddedId, @NotNull Block moddedElement, @NotNull KDLNode node, @NotNull Map<Identifier, BlockPolyEntry> resultMap, boolean regex) throws ConfigFormatException {
            List<KDLNode> nodeChildren = Utils.getChildren(node);
            List<KDLNode> mergeNodes = new ArrayList<>();
            for (KDLNode n : nodeChildren) {
                if (n.getIdentifier().equals("merge")) {
                    mergeNodes.add(n);
                }
            }
            BlockStateMerger stateMerger;
            if (mergeNodes.isEmpty()) {
                stateMerger = BlockStateMerger.DEFAULT;
            } else {
                stateMerger = a -> a; // Do nothing by default
                for (var mergeNode : mergeNodes) {
                    stateMerger = stateMerger.combine(getMergerFromNode(mergeNode, moddedElement));
                }
            }
            // The `block` node can have nested `state` children. So we're going to parse it into a tree of BlockStateSubgroup's, with the `block` node being the root
            var stateSubgroup = BlockStateSubgroup.parseNode(node, moddedElement, true);
            resultMap.put(moddedId, new BlockPolyEntry(moddedElement, regex, stateMerger, stateSubgroup));
        }

        private static BlockStateMerger getMergerFromNode(KDLNode mergeNode, Block moddedBlock) throws ConfigFormatException {
            // A blockstate merger will merge the input blockstate to a canonical version.
            // The merge node will specify a property and a range of values for that property.
            // It can also optionally specify an argument which contains the desired canonical value for that property.

            if (mergeNode.getProps().size() != 1) throw wrongAmountOfPropertiesForMergeNode(mergeNode.getProps().size());
            // The property declaration will be something like `age="1..4"`
            var propDeclaration = mergeNode.getProps().entrySet().stream().findFirst().get();
            var propertyName = propDeclaration.getKey();
            var valueRange = propDeclaration.getValue();

            var propertyWithFilter = PropertyFilter.get(propertyName, valueRange, moddedBlock);

            // Parse the canonical value
            List<KDLValue<?>> args = mergeNode.getArgs();
            if (args.size() > 1) throw wrongAmountOfArgsForMergeNode(args.size());
            Object canonicalValue;
            if (args.size() == 0) {
                try {
                    canonicalValue = findCanonicalValue(propertyWithFilter);
                } catch (IllegalStateException e) {
                    throw new ConfigFormatException("Couldn't find any properties of " + propertyName + " matching " + valueRange);
                }
            } else {
                canonicalValue = propertyWithFilter.property().parse(args.get(0).getAsString().getValue());
            }

            return state -> {
                if (propertyWithFilter.testState(state)) {
                    return uncheckedWith(state, propertyWithFilter.property(), canonicalValue);
                } else {
                    return state;
                }
            };
        }

        private static <T extends Comparable<T>> BlockState uncheckedWith(BlockState state, Property<T> property, Object value) {
            //noinspection unchecked
            return state.with(property, (T) value);
        }

        private static <T extends Comparable<T>> T findCanonicalValue(PropertyFilter.PropertyWithFilter<T> propertyWithFilter) {
            return propertyWithFilter.property()
                    .getValues()
                    .stream()
                    .filter(propertyWithFilter.filter())
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);
        }

        private static ConfigFormatException wrongAmountOfArgsForMergeNode(int v) {
            return new ConfigFormatException("Expected 1 or zero arguments, found " + v)
                    .withHelp("`merge` nodes can specify a single freestanding value which is the canonical value for the merge.")
                    .withHelp("Try looking at the examples in the README");
        }

        private static ConfigFormatException wrongAmountOfPropertiesForMergeNode(int v) {
            return new ConfigFormatException("Expected 1 property pair, found " + v)
                    .withHelp("`merge` nodes should have a single key-value pair to specify which property and which range of values for that property should be merged.")
                    .withHelp("Try looking at the examples in the README");
        }
    }
}
