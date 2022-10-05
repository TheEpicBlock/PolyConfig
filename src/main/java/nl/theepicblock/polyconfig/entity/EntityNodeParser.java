package nl.theepicblock.polyconfig.entity;

import dev.hbeck.kdl.objects.KDLNode;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import nl.theepicblock.polyconfig.Utils;
import nl.theepicblock.polyconfig.block.ConfigFormatException;

import java.util.Map;

public class EntityNodeParser {
    public static final Text EMPTY_TEXT = Text.empty();

    /**
     * Interprets a block node
     * @param resultMap the map in which the result will be added.
     */
    public static void parseEntityNode(KDLNode node, Map<Identifier, EntityEntry> resultMap) throws ConfigFormatException {
        Utils.getFromRegistry(Utils.getSingleArgNoProps(node).getAsString(), "entity", Registry.ENTITY_TYPE, (id, entity, isRegex) -> {
            if (isRegex) {
                if (!resultMap.containsKey(id)) {
                    processEntity(id, entity, node, resultMap, false);
                }
            } else {
                // Things declared as regexes can be safely overriden
                if (resultMap.containsKey(id) && !resultMap.get(id).regex()) {
                    throw Utils.duplicateEntry(id);
                }
                processEntity(id, entity, node, resultMap, false);
            }
        });
    }

    private static void processEntity(Identifier moddedId, EntityType<?> moddedEntity, KDLNode node, Map<Identifier, EntityEntry> resultMap, boolean regex) throws ConfigFormatException {
        var baseNodes = Utils.getChildren(node).stream().filter(n -> n.getIdentifier().equals("base")).toList();
        if (baseNodes.size() != 1) {
            throw new ConfigFormatException("Expected 1 base node, found "+baseNodes.size());
        }
        var baseEntityStr = Utils.getSingleArgNoProps(baseNodes.get(0)).getAsString().getValue();
        var baseEntityId = Identifier.tryParse(baseEntityStr);
        if (baseEntityId == null) throw Utils.invalidId(baseEntityStr);
        var baseEntity = Registry.ENTITY_TYPE.getOrEmpty(baseEntityId)
                .orElseThrow(() -> new ConfigFormatException("Couldn't find any entity matching "+baseEntityStr)
                        .withHelp("Try checking the spelling"));

        var nameNodes = Utils.getChildren(node).stream().filter(n -> n.getIdentifier().equals("base")).toList();
        if (nameNodes.size() > 1) {
            throw new ConfigFormatException("Expected 0 or 1 name node, found "+nameNodes.size());
        }

        Text name;
        if (!nameNodes.isEmpty()) {
            var nameArg = Utils.getSingleArgNoProps(nameNodes.get(0));
            if (nameArg.isNull()) {
                name = EMPTY_TEXT;
            } else {
                boolean isJson;
                if (nameArg.getType().isEmpty()) {
                    isJson = false;
                } else {
                    isJson = switch (nameArg.getType().get()) {
                        case "json" -> true;
                        case "literal" -> false;
                        default -> throw new ConfigFormatException("Invalid type "+nameArg.getType().get());
                    };
                }

                if (isJson) {
                    name = Text.Serializer.fromLenientJson(nameArg.getAsString().getValue());
                } else {
                    name = Text.literal(nameArg.getAsString().getValue());
                }
            }
        } else {
            name = null;
        }
        resultMap.put(moddedId, new EntityEntry(moddedEntity, baseEntity, name, regex));
    }

    public record EntityEntry(EntityType<?> moddedEntity, EntityType<?> vanillaReplacement, Text name, boolean regex) {}
}
