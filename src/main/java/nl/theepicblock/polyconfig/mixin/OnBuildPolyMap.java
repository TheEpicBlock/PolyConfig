package nl.theepicblock.polyconfig.mixin;

import io.github.theepicblock.polymc.api.PolyMap;
import io.github.theepicblock.polymc.api.PolyRegistry;
import io.github.theepicblock.polymc.api.block.BlockPoly;
import io.github.theepicblock.polymc.impl.poly.block.FunctionBlockStatePoly;
import net.minecraft.block.Block;
import nl.theepicblock.polyconfig.block.CustomBlockPoly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(PolyRegistry.class)
public class OnBuildPolyMap {
    @Shadow @Final protected Map<Block,BlockPoly> blockPolys;

    @Inject(method = "build", at = @At("HEAD"), remap = false)
    public void recheckPolys(CallbackInfoReturnable<PolyMap> cir) {
        var newBlockPolys = new HashMap<Block, BlockPoly>();

        this.blockPolys.forEach((block, poly) -> {
            if (poly instanceof CustomBlockPoly customPoly) {
                newBlockPolys.put(block, new FunctionBlockStatePoly(
                        block,
                        (state, registry) -> {
                            var oldClientState = customPoly.getClientBlock(state);
                            if (customPoly.toBeRechecked.containsKey(state) && this.blockPolys.containsKey(oldClientState.getBlock())) {
                                // Recheck the client state
                                return this.blockPolys.get(oldClientState.getBlock()).getClientBlock(state);
                            } else {
                                return oldClientState;
                            }
                        },
                        (a) -> a // Don't merge any states
                ));
            }
        });

        this.blockPolys.putAll(newBlockPolys);
    }
}
