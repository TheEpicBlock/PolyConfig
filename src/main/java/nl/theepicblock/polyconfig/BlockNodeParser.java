package nl.theepicblock.polyconfig;

import dev.hbeck.kdl.objects.KDLNode;
import io.github.theepicblock.polymc.api.block.BlockStateMerger;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

public class BlockNodeParser {
    /**
     * Interprets a block node
     * @param resultMap the map in which the result will be added.
     */
    static void parseBlockNode(KDLNode node, Map<Identifier, BlockEntry> resultMap) throws ConfigFormatException {
        var args = node.getArgs();
        if (args.size() != 1) throw wrongAmountOfArgsForBlockNode(args.size());
        if (!node.getProps().isEmpty()) throw new ConfigFormatException("Block nodes should not have any properties").withHelp("try removing any x=.. attached to the block node");

        var moddedIdString = args.get(0).getAsString().getValue();
        // Try to parse it as a regular id, otherwise consider it a regex
        var possibleId = Identifier.tryParse(moddedIdString);
        if (possibleId == null) {
            var predicate = Pattern.compile(moddedIdString).asMatchPredicate();
            var count = 0;
            for (var id : Registry.BLOCK.getIds()) {
                var exceptions = new ArrayList<ConfigFormatException>();
                if (predicate.test(id.toString())) {
                    count++;
                    if (!resultMap.containsKey(id)) {
                        try {
                            processBlock(id, Registry.BLOCK.get(id), node, resultMap, true);
                        } catch (ConfigFormatException e) {
                            exceptions.add(e);
                        }
                    }
                }

                if (!exceptions.isEmpty()) {
                    if (exceptions.size() == 1) {
                        throw exceptions.get(0);
                    } else {
                        throw new ConfigFormatException("Warning: regex caused multiple errors with multiple blocks").withSubExceptions(exceptions);
                    }
                }
            }
            if (count == 0) {
                PolyConfig.LOGGER.warn("The regex "+moddedIdString+" didn't match any blocks!");
            }
        } else {
            var moddedBlock = Registry.BLOCK.getOrEmpty(possibleId).orElseThrow(() -> invalidBlock(possibleId));
            if (resultMap.containsKey(possibleId)) {
                if (resultMap.get(possibleId).regex()) {
                    // Silently override
                    processBlock(possibleId, moddedBlock, node, resultMap, false);
                } else {
                    // If there were two explicit declaration, just crash
                    throw duplicateEntry(possibleId);
                }
            }
        }
    }

    static void processBlock(Identifier moddedId, Block moddedBlock, KDLNode node, Map<Identifier, BlockEntry> resultMap, boolean regex) throws ConfigFormatException {
        var mergeNodes = KDLUtil.getChildren(node)
                .stream()
                .filter(n -> n.getIdentifier().equals("merge"))
                .toList();
        BlockStateMerger merger;
        if (mergeNodes.isEmpty()) {
            merger = BlockStateMerger.DEFAULT;
        } else {
            merger = a -> a; // Do nothing by default
            for (var mergeNode : mergeNodes) {
                merger = merger.combine(getMergerFromNode(mergeNode, moddedBlock));
            }
        }

        // The `block` node can have nested `state` children. So we're going to parse it into a tree of BlockStateSubgroup's, with the `block` node being the root
        var rootNode = BlockStateSubgroup.parseNode(node, moddedBlock, true);
        resultMap.put(moddedId, new BlockEntry(moddedBlock, merger, rootNode, regex));
    }

    record BlockEntry(Block moddedBlock, BlockStateMerger merger, BlockStateSubgroup rootNode, boolean regex) {}

    private static BlockStateMerger getMergerFromNode(KDLNode mergeNode, Block block) throws ConfigFormatException {
        // A blockstate merger will merge the input blockstate to a canonical version.
        // The merge node will specify a property and a range of values for that property.
        // It can also optionally specify an argument which contains the desired canonical value for that property.

        if (mergeNode.getProps().size() != 1) throw wrongAmountOfPropertiesForMergeNode(mergeNode.getProps().size());
        // The property declaration will be something like `age="1..4"`
        var propDeclaration = mergeNode.getProps().entrySet().stream().findFirst().get();
        var propertyName = propDeclaration.getKey();
        var valueRange = propDeclaration.getValue();

        var propertyWithFilter = PropertyFilter.get(propertyName, valueRange, block);

        // Parse the canonical value
        if (mergeNode.getArgs().size() > 1) throw wrongAmountOfArgsForMergeNode(mergeNode.getArgs().size());
        Object canonicalValue;
        if (mergeNode.getArgs().size() == 0) {
            try {
                canonicalValue = findCanonicalValue(propertyWithFilter);
            } catch (IllegalStateException e) {
                throw new ConfigFormatException("Couldn't find any properties of "+propertyName+" matching "+valueRange);
            }
        } else {
            canonicalValue = propertyWithFilter.property().parse(mergeNode.getArgs().get(0).getAsString().getValue());
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
        return state.with(property, (T)value);
    }

    private static <T extends Comparable<T>> T findCanonicalValue(PropertyFilter.PropertyWithFilter<T> propertyWithFilter) {
        return propertyWithFilter.property()
                .getValues()
                .stream()
                .filter(propertyWithFilter.filter())
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

    private static ConfigFormatException wrongAmountOfArgsForBlockNode(int v) {
        return new ConfigFormatException("Expected 1 argument, found "+v)
                .withHelp("`block` nodes are supposed to only have a single argument, namely the identifier of the modded block.")
                .withHelp("There shouldn't be anything else between `block` and the { or the end of the line");
    }

    private static ConfigFormatException wrongAmountOfArgsForMergeNode(int v) {
        return new ConfigFormatException("Expected 1 or zero arguments, found "+v)
                .withHelp("`merge` nodes can specify a single freestanding value which is the canonical value for the merge.")
                .withHelp("Try looking at the examples in the README");
    }

    private static ConfigFormatException wrongAmountOfPropertiesForMergeNode(int v) {
        return new ConfigFormatException("Expected 1 property pair, found "+v)
                .withHelp("`merge` nodes should have a single key-value pair to specify which property and which range of values for that property should be merged.")
                .withHelp("Try looking at the examples in the README");
    }

    static ConfigFormatException invalidId(String id) {
        return new ConfigFormatException("Invalid identifier "+id);
    }

    static ConfigFormatException invalidBlock(Identifier id) {
        return new ConfigFormatException("Couldn't find any block matching "+id)
                .withHelp("Try checking the spelling");
    }

    private static ConfigFormatException duplicateEntry(Identifier id) {
        return new ConfigFormatException(id+" was already registered");
    }
}
