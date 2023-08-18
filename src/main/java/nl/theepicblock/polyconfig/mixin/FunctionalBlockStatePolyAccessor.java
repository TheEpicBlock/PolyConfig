package nl.theepicblock.polyconfig.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import io.github.theepicblock.polymc.impl.poly.block.FunctionBlockStatePoly;
import net.minecraft.block.BlockState;

@Mixin(FunctionBlockStatePoly.class)
public interface FunctionalBlockStatePolyAccessor {
    @Accessor
    ArrayList<BlockState> getUniqueClientBlocks();

}
