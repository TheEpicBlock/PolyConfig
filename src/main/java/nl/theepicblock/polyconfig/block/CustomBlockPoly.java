package nl.theepicblock.polyconfig.block;

import io.github.theepicblock.polymc.api.block.BlockStateMerger;
import io.github.theepicblock.polymc.impl.misc.BooleanContainer;
import io.github.theepicblock.polymc.impl.poly.block.FunctionBlockStatePoly;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import nl.theepicblock.polyconfig.mixin.FunctionalBlockStatePolyAccessor;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public class CustomBlockPoly extends FunctionBlockStatePoly {
    public final Set<BlockState> noRecheck;

    public CustomBlockPoly(Block moddedBlock, BiFunction<BlockState,BooleanContainer,BlockState> registrationProvider, BlockStateMerger merger) {
        super(moddedBlock, registrationProvider, merger);
        this.noRecheck = new HashSet<>();
        noRecheck.addAll(((FunctionalBlockStatePolyAccessor)this).getUniqueClientBlocks());
    }
}
