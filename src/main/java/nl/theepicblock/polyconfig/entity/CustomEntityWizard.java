package nl.theepicblock.polyconfig.entity;

import io.github.theepicblock.polymc.api.wizard.PacketConsumer;
import io.github.theepicblock.polymc.api.wizard.VirtualEntity;
import io.github.theepicblock.polymc.api.wizard.WizardInfo;
import io.github.theepicblock.polymc.impl.poly.entity.EntityWizard;
import io.github.theepicblock.polymc.impl.poly.wizard.AbstractVirtualEntity;
import io.github.theepicblock.polymc.impl.poly.wizard.EntityUtil;
import io.github.theepicblock.polymc.mixins.wizards.EntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.text.Text;
import nl.theepicblock.polyconfig.util.Utils;

import java.util.List;
import java.util.Optional;

public class CustomEntityWizard<T extends Entity> extends EntityWizard<T> {
    private final VirtualEntity virtualEntity;
    private final Text name;

    public CustomEntityWizard(WizardInfo info, T entity, EntityType<?> vanillaEntityType, Text name) {
        super(info, entity);
        this.name = name;
        this.virtualEntity = new AbstractVirtualEntity(entity.getUuid(), entity.getId()) {
            @Override
            public EntityType<?> getEntityType() {
                return vanillaEntityType;
            }
        };
    }

    @Override
    public void addPlayer(PacketConsumer player) {
        virtualEntity.spawn(player, this.getPosition());

        if (this.name != Utils.EMPTY_TEXT) {
            player.sendPacket(EntityUtil.createDataTrackerUpdate(
                    this.virtualEntity.getId(),
                    List.of(
                            new DataTracker.Entry<>(EntityAccessor.getCustomName(), Optional.of(name == null ? this.getEntity().getName() : name)),
                            new DataTracker.Entry<>(EntityAccessor.getNameVisible(), true))
                    )
            );
        }
    }

    @Override
    public void removePlayer(PacketConsumer player) {
        virtualEntity.remove(player);
    }
}
