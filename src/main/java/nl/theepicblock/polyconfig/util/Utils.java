package nl.theepicblock.polyconfig.util;

import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.objects.KDLString;
import dev.hbeck.kdl.objects.KDLValue;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import nl.theepicblock.polyconfig.PolyConfig;
import nl.theepicblock.polyconfig.block.ConfigFormatException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Utils {
    public static final Text EMPTY_TEXT = Text.empty();

    public static List<KDLNode> getChildren(KDLNode node) {
        var childDoc = node.getChild();
        if (childDoc.isPresent()) {
            return childDoc.get().getNodes();
        } else {
            return Collections.emptyList();
        }
    }

    public static <T> void getFromRegistry(KDLString string, String v, Registry<T> registry, GetFromRegistryConsumer<T> consumer) throws ConfigFormatException {
        boolean isRegex;
        if (string.getType().isEmpty()) {
            // Automatically determine if it's a regex
            isRegex = Identifier.tryParse(string.getValue()) == null;
        } else {
            isRegex = switch (string.getType().get()) {
                case "identifier" -> false;
                case "id" -> false;
                case "regex" -> true;
                default -> throw new ConfigFormatException("Invalid type "+string.getType().get());
            };
        }
        if (isRegex) {
            var predicate = Pattern.compile(string.getValue()).asMatchPredicate();
            var count = 0;
            var exceptions = new ArrayList<ConfigFormatException>();
            for (var id : registry.getIds()) {
                if (predicate.test(id.toString())) {
                    count++;
                    try {
                        consumer.accept(id, registry.get(id), true);
                    } catch (ConfigFormatException e) {
                        exceptions.add(e);
                    }
                }
            }

            if (!exceptions.isEmpty()) {
                if (exceptions.size() == 1) {
                    throw exceptions.get(0);
                } else {
                    throw new ConfigFormatException("Warning: regex caused multiple errors with multiple "+v+"s").withSubExceptions(exceptions);
                }
            }

            if (count == 0) {
                PolyConfig.LOGGER.warn("The regex "+string.getValue()+" didn't match any "+v+"s!");
            }
        } else {
            var id = new Identifier(string.getValue());
            var value = registry
                    .getOrEmpty(id)
                    .orElseThrow(() -> notFoundInRegistry(id, v));
            consumer.accept(id, value, false);
        }
    }

    @FunctionalInterface
    public interface GetFromRegistryConsumer<T> {
        void accept(Identifier id, T v, boolean isRegex) throws ConfigFormatException;
    }

    public static KDLNode getSingleNode(List<KDLNode> nodes, String name) throws ConfigFormatException {
        KDLNode result = null;
        for (KDLNode node : nodes) {
            if (node.getIdentifier().equals(name)) {
                if (result != null) {
                    throw new ConfigFormatException("Duplicate node "+name+", only one is allowed");
                }
                result = node;
            }
        }
        if (result == null) {
            throw new ConfigFormatException("Expected node "+name);
        }
        return result;
    }

    public static KDLNode getOptionalNode(List<KDLNode> nodes, String name) throws ConfigFormatException {
        KDLNode result = null;
        for (KDLNode node : nodes) {
            if (node.getIdentifier().equals(name)) {
                if (result != null) {
                    throw new ConfigFormatException("Duplicate node "+name+", zero or one is allowed");
                }
                result = node;
            }
        }
        return result;
    }

    public static KDLValue<?> getSingleArgNoProps(KDLNode node) throws ConfigFormatException {
        if (node.getArgs().size() != 1) {
            throw wrongAmountOfArgsForNode(node.getArgs().size(), node.getIdentifier());
        }
        if (!node.getProps().isEmpty()) throw new ConfigFormatException(node.getIdentifier()+" nodes should not have any properties").withHelp("try removing any x=.. attached to the node");
        return node.getArgs().get(0);
    }

    public static Text getText(KDLValue<?> value) throws ConfigFormatException {
        return getText(value, null);
    }

    public static Text getText(KDLValue<?> value, @Nullable Style defaultStyle) throws ConfigFormatException {
        if (value.isNull()) {
            return EMPTY_TEXT;
        } else {
            boolean isJson;
            if (value.getType().isEmpty()) {
                isJson = false;
            } else {
                isJson = switch (value.getType().get()) {
                    case "json" -> true;
                    case "literal" -> false;
                    default -> throw new ConfigFormatException("Invalid type "+value.getType().get());
                };
            }

            if (isJson) {
                return Text.Serializer.fromLenientJson(value.getAsString().getValue());
            } else {
                return Text.literal(value.getAsString().getValue()).setStyle(defaultStyle);
            }
        }
    }

    public static @NotNull Identifier parseIdentifier(String s) throws ConfigFormatException {
        try {
            return new Identifier(s);
        } catch (IllegalArgumentException e) {
            throw new ConfigFormatException("Illegal identifier "+s)
                    .withHelp("Try checking the spelling: "+e.getMessage());
        }
    }

    public static ConfigFormatException wrongAmountOfArgsForNode(int v, String nodeName) {
        return new ConfigFormatException("Expected 1 argument, found "+v)
                .withHelp("`"+nodeName+"` nodes are supposed to only have a single argument, namely the identifier of the modded "+nodeName+".")
                .withHelp("There shouldn't be anything else between `"+nodeName+"` and the { or the end of the line");
    }

    public static ConfigFormatException duplicateEntry(Identifier id) {
        return new ConfigFormatException(id+" was already registered");
    }

    public static ConfigFormatException notFoundInRegistry(Identifier id, String type) {
        return new ConfigFormatException("Couldn't find any "+type+" matching "+id)
                .withHelp("Try checking the spelling");
    }
}
