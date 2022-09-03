package nl.theepicblock.polyconfig;

import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.parse.KDLParseException;
import dev.hbeck.kdl.parse.KDLParser;
import io.github.theepicblock.polymc.api.PolyMcEntrypoint;
import io.github.theepicblock.polymc.api.PolyRegistry;
import io.github.theepicblock.polymc.api.block.BlockStateManager;
import io.github.theepicblock.polymc.api.item.ItemLocation;
import io.github.theepicblock.polymc.api.item.ItemPoly;
import io.github.theepicblock.polymc.api.resource.ModdedResources;
import io.github.theepicblock.polymc.api.resource.PolyMcResourcePack;
import io.github.theepicblock.polymc.impl.misc.logging.SimpleLogger;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nl.theepicblock.polyconfig.block.BlockPolyEntry;
import nl.theepicblock.polyconfig.block.ConfigFormatException;
import nl.theepicblock.polyconfig.block.CustomBlockPoly;
import nl.theepicblock.polyconfig.entity.CustomEntityWizard;
import nl.theepicblock.polyconfig.entity.EntityPolyEntry;
import nl.theepicblock.polyconfig.item.ItemPolyEntry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PolyConfig implements PolyMcEntrypoint {
    private static final int CURRENT_VERSION = 1;
    public static final Logger LOGGER = LoggerFactory.getLogger("polyconfig");

    /**
     * A temporary record that holds the parsed nodes before they're applied
     */
    public record Declarations(Map<Identifier, ItemPolyEntry> itemDeclarations, Map<Identifier, BlockPolyEntry> blockDeclarations, Map<Identifier, EntityPolyEntry> entityDeclarations) {}

    @Override
    public void registerPolys(PolyRegistry registry) {
        var parser = new KDLParser();
        var declarations = new Declarations(new HashMap<>(), new HashMap<>(), new HashMap<>());
        var configdir = FabricLoader.getInstance().getConfigDir();

        var oldLocation = configdir.resolve("polyconfig.kdl").toFile();
        var polyconfigdir = configdir.resolve("polyconfig");
        if (oldLocation.exists()) {
            // Still uses the old location, respect that
            handleFile(oldLocation, parser, declarations);
        } else {
            // Try see if there's a file at the new location, if not we should create a directory
            if (!polyconfigdir.toFile().exists()) {
                polyconfigdir.toFile().mkdirs();
                try {
                    Files.copy(FabricLoader.getInstance().getModContainer("polyconfig").orElseThrow().findPath("defaultconfig.kdl").orElseThrow(), polyconfigdir.resolve("main.kdl"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            if (Files.exists(polyconfigdir)) {
                Files.walk(polyconfigdir, Integer.MAX_VALUE, FileVisitOption.FOLLOW_LINKS).forEachOrdered(path -> {
                    if (Files.isDirectory(path)) return;
                    handleFile(path.toFile(), parser, declarations);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Apply block nodes to PolyMc
        declarations.blockDeclarations().forEach((identifier, blockEntry) -> {
            registry.registerBlockPoly(
                    blockEntry.getModded(),
                    new CustomBlockPoly(
                            blockEntry.getModded(),
                            (state, isUniqueCallback) -> blockEntry.getStateSubgroup().grabBlockState(state, isUniqueCallback, registry.getSharedValues(BlockStateManager.KEY)),
                            blockEntry.getStateMerger()));
        });

        // Apply item nodes
        declarations.itemDeclarations().forEach((identifier, itemEntry) -> {
            var poly = itemEntry.getPolyProvider().create(registry, itemEntry.getModded());

            if (itemEntry.getStackModifiers().isEmpty()) {
                registry.registerItemPoly(itemEntry.getModded(), poly);
                return;
            }

            registry.registerItemPoly(itemEntry.getModded(), new ItemPoly() {
                @Override
                public ItemStack getClientItem(ItemStack input, @Nullable ServerPlayerEntity player, @Nullable ItemLocation location) {
                    ItemStack stack = poly.getClientItem(input, player, location);
                    for (Consumer<ItemStack> modifier : itemEntry.getStackModifiers()) {
                        modifier.accept(stack);
                    }
                    return stack;
                }

                @Override
                public void addToResourcePack(Item item, ModdedResources moddedResources, PolyMcResourcePack pack, SimpleLogger logger) {
                    poly.addToResourcePack(item, moddedResources, pack, logger);
                }
            });
        });

        // Apply entity nodes
        declarations.entityDeclarations().forEach((identifier, entityEntry) -> {
            registry.registerEntityPoly(entityEntry.getModded(), (info, entity) -> new CustomEntityWizard<>(info, entity, entityEntry.getBase(), entityEntry.getName()));
        });
    }

    private static void handleFile(File configFile, KDLParser parser, Declarations declarations) {
        var path = FabricLoader.getInstance().getConfigDir().relativize(configFile.toPath());
        try (var stream = new FileInputStream(configFile)) {
            var config = parser.parse(stream);
            stream.close();

            // Check version
            var versionNodes = config.getNodes().stream().filter(node -> node.getIdentifier().equals("version")).toList();
            if (versionNodes.size() != 1) throw multipleVersionDeclarations(versionNodes.size());
            var versionNode = versionNodes.get(0);
            if (versionNode.getArgs().size() != 1) throw invalidVersionArgs(versionNode.getArgs().size());
            var version = versionNode.getArgs().get(0).getAsNumber().orElseThrow();
            if (version.getValue().intValue() != CURRENT_VERSION) throw unsupportedVersion(version.getValue());

            // Loop through config nodes
            for (var node : config.getNodes()) {
                // These errors are only warnings, so we wrap this in another try block
                try {
                    switch (node.getIdentifier()) {
                        case "version" -> {}
                        case "block" -> BlockPolyEntry.PARSER.parseNode(node, declarations.blockDeclarations);
                        case "item" -> ItemPolyEntry.PARSER.parseNode(node, declarations.itemDeclarations);
                        case "entity" -> EntityPolyEntry.PARSER.parseNode(node, declarations.entityDeclarations);
                        default -> throw unknownNode(node);
                    }
                } catch (ConfigFormatException e) {
                    LOGGER.warn("(polyconfig) " + e
                            .withHelp("the offending node was located in " + path)
                            .withHelp("the offending node looked something like this (formatting may differ)\n    | " + node.toKDL().replace("\n", "\n    | "))
                            .toString());
                }
            }
        } catch (IOException e) {
            LOGGER.error("(polyconfig) Couldn't read config ("+path+")", e);
        } catch (KDLParseException e) {
            LOGGER.error("(polyconfig) Invalid config file ("+path+")", e);
        } catch (ConfigFormatException e) {
            LOGGER.error("(polyconfig) error in "+path+": "+e);
        }
    }

    private static ConfigFormatException multipleVersionDeclarations(int found) {
        return new ConfigFormatException("Expected 1 version declaration. Found "+found)
                .withHelp("Your config file includes `version ...` multiple times. Try deleting all but the topmost one.");
    }

    private static ConfigFormatException invalidVersionArgs(int found) {
        return new ConfigFormatException("Invalid number of arguments in version declaration. Expected 1, found "+found)
                .withHelp("Your config should include `version "+CURRENT_VERSION+"` at the top. There's supposed to be a single number there");
    }

    private static ConfigFormatException unsupportedVersion(Number v) {
        return new ConfigFormatException("Version "+v+" is not supported");
    }

    private static ConfigFormatException unknownNode(KDLNode node) {
        return new ConfigFormatException(node.getIdentifier()+" is not a recognized node type")
                .withHelp("try removing it");
    }
}
