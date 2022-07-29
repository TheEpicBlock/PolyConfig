package nl.theepicblock.polyconfig.block;

import io.github.theepicblock.polymc.api.block.BlockStateMerger;
import io.github.theepicblock.polymc.impl.misc.BooleanContainer;
import io.github.theepicblock.polymc.impl.poly.block.FunctionBlockStatePoly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CustomBlockPoly extends FunctionBlockStatePoly {
    public final Map<BlockState, BlockState> toBeRechecked;

    public CustomBlockPoly(Block moddedBlock, BiFunction<BlockState,BooleanContainer,BlockState> registrationProvider, BlockStateMerger merger) {
        this(moddedBlock, registrationProvider, merger, new HashMap<>());
    }

    public CustomBlockPoly(Block moddedBlock, BiFunction<BlockState,BooleanContainer,BlockState> registrationProvider, BlockStateMerger merger, Map<BlockState, BlockState> weirdHackAroundConstructorStuff) {
        super(moddedBlock, (state, isUniqueCallback) -> {
            var newBlock = registrationProvider.apply(state, isUniqueCallback);
            if (!isUniqueCallback.get()) {
                // So this block isn't supposed to be unique
                // We're going to add it to a list, so we can check it later
                // This is for cases such as when someone replaces a slab with a spruce slab
                // Because PolyMc uses double slabs for custom textures, we should replace it with the full block (See BlockStateProfile#DOUBLE_SLAB_SUB_PROFILE)
                // To achieve this we'll iterate over the list at some point
                weirdHackAroundConstructorStuff.put(state, newBlock);
            }
            return newBlock;
        }, merger);
        this.toBeRechecked = weirdHackAroundConstructorStuff;
    }
}
