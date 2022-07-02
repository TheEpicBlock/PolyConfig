package nl.theepicblock.polyconfig;

import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.objects.KDLValue;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public record BlockStateSubgroup(Predicate<BlockState> filter, List<BlockStateSubgroup> children, List<BlockReplaceReference> replaces) {
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

        for (var child : KDLUtil.getChildren(node)) {
            switch (child.getIdentifier()) {
                case "merge" -> { if (isRoot) { throw invalidMerge(child); } }
                case "state" -> children.add(parseNode(child, moddedBlock, false));
                case "replace" -> replaceEntries.add(parseReplaceNode(node));
                default -> throw unknownNode(child);
            }
        }

        return new BlockStateSubgroup(filter, children, replaceEntries);
    }

    private static BlockReplaceReference parseReplaceNode(KDLNode node) throws ConfigFormatException {
        var args = node.getArgs();
        if (args.size() != 1) throw wrongAmountOfArgsForReplaceNode(args.size());
        var replacementArg = args.get(0);
        var replacementArgAsString = replacementArg.getAsString().getValue();
        var replacementArgType = replacementArg.getType().orElse("state");

        if (replacementArgType.equals("state")) {
            var id = Identifier.tryParse(replacementArgAsString);
            if (id == null) throw BlockNodeParser.invalidId(replacementArgAsString);
            var block = Registry.BLOCK.getOrEmpty(id).orElseThrow(() -> BlockNodeParser.invalidBlock(id));
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
        } else if (replacementArgType.equals("group")) {
            return new BlockReplaceReference.BlockGroupReference(
                    Arrays.stream(BlockGroup.values())
                            .filter(group -> group.name.equals(replacementArgAsString))
                            .findFirst()
                            .orElseThrow(() ->
                                    new ConfigFormatException(replacementArgAsString+" is not a valid group.")
                                            .withHelp("valid groups are: "+ Arrays.stream(BlockGroup.values()).map(group -> group.name).collect(Collectors.joining(", ", "[","]")))));
        } else {
            throw new ConfigFormatException(replacementArgType+" is an invalid type for a replace node's argument. Should be either `state` or `group`");
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
