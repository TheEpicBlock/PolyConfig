package nl.theepicblock.polyconfig.entity;

import dev.hbeck.kdl.objects.KDLNode;
import net.minecraft.entity.EntityType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import nl.theepicblock.polyconfig.block.ConfigFormatException;
import nl.theepicblock.polyconfig.util.PolyConfigEntry;
import nl.theepicblock.polyconfig.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class EntityPolyEntry extends PolyConfigEntry<EntityType<?>> {
    public static final Parser PARSER = new Parser();

    @NotNull
    private final EntityType<?> base;
    @Nullable
    private final Text name;

    public EntityPolyEntry(@NotNull EntityType<?> moddedElement, boolean regex, @NotNull EntityType<?> base, @Nullable Text name) {
        super(moddedElement, regex);
        this.base = base;
        this.name = name;
    }

    public @NotNull EntityType<?> getBase() {
        return base;
    }

    public @Nullable Text getName() {
        return name;
    }

    public static class Parser extends PolyConfigEntry.Parser<EntityType<?>, EntityPolyEntry> {
        public Parser() {
            super(Registry.ENTITY_TYPE, "entity");
        }

        @Override
        public void process(@NotNull Identifier moddedId, @NotNull EntityType<?> moddedElement, @NotNull KDLNode node, @NotNull Map<Identifier, EntityPolyEntry> resultMap, boolean regex) throws ConfigFormatException {
            var nodeChildren = Utils.getChildren(node);
            var baseEntity = resolveElement(Utils.getSingleArgNoProps(Utils.getSingleNode(nodeChildren, "base")).getAsString().getValue());
            var nameNode = Utils.getOptionalNode(nodeChildren, "name");
            Text name = null;
            if (nameNode != null) {
                name = Utils.getText(Utils.getSingleArgNoProps(nameNode).getAsString());
            }
            resultMap.put(moddedId, new EntityPolyEntry(baseEntity, regex, baseEntity, name));
        }
    }
}
