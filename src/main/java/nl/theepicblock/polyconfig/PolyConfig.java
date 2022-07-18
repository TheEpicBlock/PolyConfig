package nl.theepicblock.polyconfig;

import dev.hbeck.kdl.objects.KDLNode;
import dev.hbeck.kdl.parse.KDLParseException;
import dev.hbeck.kdl.parse.KDLParser;
import io.github.theepicblock.polymc.api.PolyMcEntrypoint;
import io.github.theepicblock.polymc.api.PolyRegistry;
import io.github.theepicblock.polymc.api.block.BlockStateManager;
import io.github.theepicblock.polymc.impl.poly.block.FunctionBlockStatePoly;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.stream.Collectors;

public class PolyConfig implements PolyMcEntrypoint {
	private static final int CURRENT_VERSION = 1;
	public static final Logger LOGGER = LoggerFactory.getLogger("polyconfig");

	@Override
	public void registerPolys(PolyRegistry registry) {
		var parser = new KDLParser();
		try (var configFile = readConfigFile()) {
			var config = parser.parse(configFile);

			// Check version
			var versionNodes = config.getNodes().stream().filter(node -> node.getIdentifier().equals("version")).toList();
			if (versionNodes.size() != 1) throw multipleVersionDeclarations(versionNodes.size());
			var versionNode = versionNodes.get(0);
			if (versionNode.getArgs().size() != 1) throw invalidVersionArgs(versionNode.getArgs().size());
			var version = versionNode.getArgs().get(0).getAsNumber().orElseThrow();
			if (version.getValue().intValue() != CURRENT_VERSION) throw unsupportedVersion(version.getValue());

			var blockDeclarations = new HashMap<Identifier, BlockNodeParser.BlockEntry>();
			// Loop through config nodes
			for (var node : config.getNodes()) {
				// These errors are only warnings, so we wrap this in another try block
				try {
					switch (node.getIdentifier()) {
						case "version" -> {}
						case "block" -> BlockNodeParser.parseBlockNode(node, blockDeclarations);
						case "item" -> handleItemNode(node);
						default -> throw unknownNode(node);
					}
				} catch (ConfigFormatException e) {
					LOGGER.warn("(polyconfig) "+
							e.withHelp("the offending node looked something like this (formatting may differ)\n    | "+node.toKDL().replace("\n", "\n    | "))
								.toString());
				}
			}

			// Apply block nodes to PolyMc
			blockDeclarations.forEach((identifier, blockEntry) -> {
				registry.registerBlockPoly(
						blockEntry.moddedBlock(),
						new FunctionBlockStatePoly(
								blockEntry.moddedBlock(),
								(state, isUniqueCallback) -> blockEntry.rootNode().grabBlockState(state, isUniqueCallback, registry.getSharedValues(BlockStateManager.KEY)),
								blockEntry.merger()));
			});

		} catch (IOException e) {
			LOGGER.error("(polyconfig) Couldn't read config", e);
		} catch (KDLParseException e) {
			LOGGER.error("(polyconfig) Invalid config file", e);
		} catch (ConfigFormatException e) {
			LOGGER.error("(polyconfig) "+e);
		}
	}

	private static void handleItemNode(KDLNode node) {

	}

	private static InputStream readConfigFile() throws IOException {
		var path = FabricLoader.getInstance().getConfigDir().resolve("polyconfig.kdl");
		if (!Files.exists(path)) {
			Files.copy(FabricLoader.getInstance().getModContainer("polyconfig").orElseThrow().findPath("defaultconfig.kdl").orElseThrow(), path);
		}
		return new FileInputStream(path.toFile());
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
