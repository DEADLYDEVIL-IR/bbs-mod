package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.events.ModelBlockEntityUpdateCallback;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ModelBlockEntity extends BlockEntity
{
    private ModelProperties properties = new ModelProperties();
    private IEntity entity = new StubEntity();

    public ModelBlockEntity(BlockPos pos, BlockState state)
    {
        super(BBSMod.MODEL_BLOCK_ENTITY, pos, state);
    }

    public ModelProperties getProperties()
    {
        return this.properties;
    }

    public IEntity getEntity()
    {
        return this.entity;
    }

    public void tick(World world, BlockPos pos, BlockState state)
    {
        ModelBlockEntityUpdateCallback.EVENT.invoker().update(this);

        this.entity.update();
        this.entity.setWorld(world);
        this.properties.update(this.entity);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket()
    {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt()
    {
        return createNbt();
    }

    @Override
    protected void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);

        DataStorageUtils.writeToNbtCompound(nbt, "Properties", this.properties.toData());
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);

        BaseType baseType = DataStorageUtils.readFromNbtCompound(nbt, "Properties");

        if (baseType instanceof MapType)
        {
            this.properties.fromData(baseType.asMap());
        }
    }

    public void updateForm(MapType data, World world)
    {
        this.properties.fromData(data);

        BlockPos pos = this.getPos();
        BlockState blockState = world.getBlockState(pos);

        world.updateListeners(pos, blockState, blockState, Block.NOTIFY_LISTENERS);
        world.markDirty(pos);
    }
}