package me.shedaniel.materialisation.blocks;

import me.shedaniel.materialisation.Materialisation;
import me.shedaniel.materialisation.ModReference;
import net.fabricmc.fabric.api.container.ContainerProviderRegistry;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.tool.attribute.v1.FabricToolTags;
import net.minecraft.block.*;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class MaterialisingTableBlock extends HorizontalFacingBlock {
    
    private static final VoxelShape SHAPE;
    
    static {
        VoxelShape base = Block.createCuboidShape(0, 0, 0, 16, 7, 16);
        SHAPE = VoxelShapes.union(base, Block.createCuboidShape(1, 7, 1, 15, 9, 15), Block.createCuboidShape(5, 9, 5, 11, 16, 11));
    }
    
    public MaterialisingTableBlock() {
        super(FabricBlockSettings.of(Material.METAL, MaterialColor.WHITE).strength(5.0F, 1200.0F).breakByTool(FabricToolTags.PICKAXES).drops(new Identifier(ModReference.MOD_ID, "blocks/materialising_table")));
        this.setDefaultState(this.stateManager.getDefaultState().with(FACING, Direction.NORTH));
    }
    
    @Override
    public BlockState getPlacementState(ItemPlacementContext placementContext) {
        return getDefaultState().with(FACING, placementContext.getPlayerFacing().getOpposite());
    }
    
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(BlockState blockState, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult) {
        if (!world.isClient)
            ContainerProviderRegistry.INSTANCE.openContainer(Materialisation.MATERIALISING_TABLE_CONTAINER, player, buf -> buf.writeBlockPos(pos));
        return ActionResult.SUCCESS;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public BlockRenderType getRenderType(BlockState blockState_1) {
        return BlockRenderType.MODEL;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public boolean hasSidedTransparency(BlockState blockState_1) {
        return true;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public boolean canPathfindThrough(BlockState blockState_1, BlockView blockView_1, BlockPos blockPos_1, NavigationType blockPlacementEnvironment_1) {
        return false;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getOutlineShape(BlockState blockState, BlockView blockView, BlockPos blockPos, ShapeContext entityContext) {
        return SHAPE;
    }
}
