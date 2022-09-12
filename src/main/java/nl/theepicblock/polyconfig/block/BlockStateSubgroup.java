package nl.theepicblock.polyconfig.block;

import dev.hbeck.kdl.objects.KDLNode;
import io.github.theepicblock.polymc.api.block.BlockStateManager;
import io.github.theepicblock.polymc.api.block.BlockStateProfile;
import io.github.theepicblock.polymc.impl.generator.BlockPolyGenerator;
import io.github.theepicblock.polymc.impl.misc.BooleanContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Property;
import net.minecraft.util.registry.Registry;
import nl.theepicblock.polyconfig.PolyConfig;
import nl.theepicblock.polyconfig.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record BlockStateSubgroup(Predicate<BlockState> filter, List<BlockStateSubgroup> children, List<BlockReplaceReference> replaces) {
    /**
     * Allocates the right clientside blockstate for the modded input blockstate according to the rules defined in here.
     */
    public BlockState grabBlockState(BlockState input, BooleanContainer isUniqueCallback, BlockStateManager stateManager) {
        for (var child : this.children) {
            if (child.filter.test(input)) {
                return child.grabBlockState(input, isUniqueCallback, stateManager);
            }
        }

        if (this.replaces.isEmpty()) {
            // Let PolyMc figure it out automagically
            return BlockPolyGenerator.registerClientState(input, isUniqueCallback, stateManager);
        }

        for (var replaceStatement : this.replaces) {
            var result = replaceStatement.tryAllocate(input, isUniqueCallback, stateManager);
            if (result != null) {
                return result;
            }
        }

        PolyConfig.LOGGER.info("(polyconfig) couldn't allocate any blocks for "+input+", we probably ran out of states");
        isUniqueCallback.set(false);
        return Blocks.STONE.getDefaultState();
    }

    public static BlockStateSubgroup parseNode(KDLNode node, Block moddedBlock, boolean isRoot) throws ConfigFormatException {
        Predicate<BlockState> filter;
        if (isRoot) {
            // The root node is the `block` node, which currently can't do any filtering
            filter = o -> true;
        } else {
            filter = o -> true;
            for (var entry : node.getProps().entrySet()) {
                var propFilter = PropertyFilter.get(entry.getKey(), entry.getValue(), moddedBlock);
                filter = filter.and(propFilter::testState);
            }
        }

        var children = new ArrayList<BlockStateSubgroup>();
        var replaceEntries = new ArrayList<BlockReplaceReference>();

        for (var child : Utils.getChildren(node)) {
            switch (child.getIdentifier()) {
                case "merge" -> { if (!isRoot) { throw invalidMerge(child); } }
                case "state" -> children.add(parseNode(child, moddedBlock, false));
                case "replace" -> replaceEntries.add(parseReplaceNode(child));
                default -> throw unknownNode(child);
            }
        }

        return new BlockStateSubgroup(filter, children, replaceEntries);
    }

    private static BlockReplaceReference parseReplaceNode(KDLNode node) throws ConfigFormatException {
        var args = node.getArgs();
        if (args.size() != 1) throw wrongAmountOfArgsForReplaceNode(args.size());
        var replacementArg = args.get(0);
        var replacementString = replacementArg.getAsString().getValue();
        var replacementType = replacementArg.getType().orElse("state");

        if (replacementType.equals("state")) {
            var id = Utils.parseIdentifier(replacementString);
            var block = Registry.BLOCK.getOrEmpty(id).orElseThrow(() -> Utils.notFoundInRegistry(id, "block"));
            var forcedValues = new ArrayList<Property.Value<?>>();

            for (var entry : node.getProps().entrySet()) {
                var propertyString = entry.getKey();
                var valueString = entry.getValue().getAsString().getValue();
                var property = block.getStateManager().getProperty(propertyString);
                if (property == null) throw PropertyFilter.propertyNotFound(propertyString, block);
                var value = parseAndGetValue(property, valueString);
                forcedValues.add(value);
            }

            return new BlockReplaceReference.BlockReference(block, forcedValues);
        } else if (replacementType.equals("group")) {
            // TODO support property filters on these
            if (!node.getProps().isEmpty()) throw new ConfigFormatException("Filtering block groups is not yet supported");
            return new BlockReplaceReference.BlockGroupReference(
                    BlockGroupUtil.tryGet(replacementString)
                            .orElseThrow(() ->
                                    new ConfigFormatException(replacementString+" is not a valid group.")
                                            .withHelp("valid groups are: "+ BlockStateProfile.ALL_PROFILES.stream().map(group -> group.name).collect(Collectors.joining(", ", "[","]")))));
        } else {
            throw new ConfigFormatException(replacementType+" is an invalid type for a replace node's argument. Should be either `state` or `group`");
        }
    }

    private static <T extends Comparable<T>> Property.Value<T> parseAndGetValue(Property<T> property, String valueString) throws ConfigFormatException {
        return property.createValue(property.parse(valueString).orElseThrow(() -> invalidPropertyValue(property.getName(), valueString)));
    }

    private static ConfigFormatException invalidPropertyValue(String propertyName, String valueName) {
        return new ConfigFormatException("Property `"+propertyName+"` does not have a value of `"+valueName+"`");
    }

    private static ConfigFormatException wrongAmountOfArgsForReplaceNode(int v) {
        return new ConfigFormatException("Expected 1 argument, found "+v)
                .withHelp("`replace` nodes are supposed to only have a single argument. (they may have more than one property though)")
                .withHelp("There shouldn't be anything else between `replace` and the end of the line");
    }

    private static ConfigFormatException invalidMerge(KDLNode node) {
        return new ConfigFormatException(node.getIdentifier()+" is not valid in this context")
                .withHelp("try moving it to the `block` level");
    }

    private static ConfigFormatException unknownNode(KDLNode node) {
        return new ConfigFormatException(node.getIdentifier()+" is not a recognized node type")
                .withHelp("try removing it");
    }
}
