package nl.theepicblock.polyconfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;

import java.util.List;

// Me: can we have Rust-like enums?
// Mom: we've got Rust-like enums at home
// Rust-like enums at home:

public interface BlockReplaceReference {
    record BlockGroupReference(BlockGroup group) implements BlockReplaceReference {}
    record BlockReference(Block block, List<Property.Value<?>> forcedValue) implements BlockReplaceReference {}
}
