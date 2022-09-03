package nl.theepicblock.polyconfig.util;

import dev.hbeck.kdl.objects.KDLNode;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import nl.theepicblock.polyconfig.block.ConfigFormatException;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PolyConfigEntry<T> {
    @NotNull
    private final T moddedElement;
    private final boolean regex;

    public PolyConfigEntry(@NotNull T moddedElement, boolean regex) {
        this.moddedElement = moddedElement;
        this.regex = regex;
    }

    public @NotNull T getModded() {
        return moddedElement;
    }

    public boolean isRegexBased() {
        return regex;
    }

    public static abstract class Parser<T, E extends PolyConfigEntry<T>> {
        @NotNull
        private final Registry<T> registry;
        @NotNull
        private final String elementType;

        protected Parser(@NotNull Registry<T> registry, @NotNull String elementType) {
            this.registry = registry;
            this.elementType = elementType;
        }

        public void parseNode(@NotNull KDLNode node, @NotNull Map<Identifier, E> resultMap) throws ConfigFormatException {
            Utils.getFromRegistry(Utils.getSingleArgNoProps(node).getAsString(), elementType, registry, (id, element, isRegex) -> {
                if (isRegex) {
                    if (!resultMap.containsKey(id)) {
                        process(id, element, node, resultMap, true);
                    }
                } else {
                    // Things declared as regexes can be safely overridden
                    if (resultMap.containsKey(id) && !resultMap.get(id).isRegexBased()) {
                        throw Utils.duplicateEntry(id);
                    }
                    process(id, element, node, resultMap, false);
                }
            });
        }

        public abstract void process(@NotNull Identifier moddedId, @NotNull T moddedElement, @NotNull KDLNode node, @NotNull Map<Identifier, E> resultMap, boolean regex) throws ConfigFormatException;

        protected T resolveElement(@NotNull String idString) throws ConfigFormatException {
            var id = Utils.parseIdentifier(idString);
            return registry.getOrEmpty(id).orElseThrow(() -> Utils.notFoundInRegistry(id, elementType));
        }
    }
}
