package nl.theepicblock.polyconfig;

import io.github.theepicblock.polymc.api.block.BlockStateManager;
import io.github.theepicblock.polymc.impl.misc.BooleanContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import org.jetbrains.annotations.Nullable;

import java.util.List;

// Me: can we have Rust-like enums?
// Mom: we've got Rust-like enums at home
// Rust-like enums at home:

public interface BlockReplaceReference {
    @Nullable BlockState tryAllocate(BlockState input, BooleanContainer isUniqueCallback, BlockStateManager stateManager);

    record BlockGroupReference(BlockGroup group) implements BlockReplaceReference {
        @Override
        public @Nullable BlockState tryAllocate(BlockState input, BooleanContainer isUniqueCallback, BlockStateManager stateManager) {
            try {
                isUniqueCallback.set(true);
                return stateManager.requestBlockState(this.group.profile);
            } catch (BlockStateManager.StateLimitReachedException e) {
                isUniqueCallback.set(false);
                return null;
            }
        }
    }
    record BlockReference(Block block, List<Property.Value<?>> forcedValues) implements BlockReplaceReference {
        @Override
        public @Nullable BlockState tryAllocate(BlockState input, BooleanContainer isUniqueCallback, BlockStateManager stateManager) {
            var state = block.getDefaultState();

            for (var inputProperty : input.getBlock().getStateManager().getProperties()) {
                try {
                    state = copy(state, inputProperty, input);
                } catch (IllegalArgumentException ignored) {}
            }

            for (var value : forcedValues) {
                state = with(state, value);
            }

            return state;
        }

        private static <T extends Comparable<T>> BlockState with(BlockState in, Property.Value<T> value) {
            return in.with(value.property(), value.value());
        }

        private static <T extends Comparable<T>> BlockState copy(BlockState in, Property<T> property, BlockState toCopy) {
            return in.with(property, toCopy.get(property));
        }
    }
}
