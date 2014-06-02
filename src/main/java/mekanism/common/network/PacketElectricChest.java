package mekanism.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import mekanism.api.Coord4D;
import mekanism.api.energy.IEnergizedItem;
import mekanism.common.IElectricChest;
import mekanism.common.Mekanism;
import mekanism.common.PacketHandler;
import mekanism.common.block.BlockMachine.MachineType;
import mekanism.common.inventory.InventoryElectricChest;
import mekanism.common.tile.TileEntityElectricChest;
import mekanism.common.util.MekanismUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public class PacketElectricChest extends MekanismPacket
{
	public ElectricChestPacketType activeType;

	public boolean isBlock;

	public boolean locked;

	public String password;

	public int guiType;
	public int windowId;

	public boolean useEnergy;

	public Coord4D obj;

	//This is a really messy implementation...
	public PacketElectricChest(ElectricChestPacketType type, boolean b1, boolean b2, int i1, int i2, String s1, Coord4D c1)
	{
		activeType = type;

		switch(activeType)
		{
			case LOCK:
				locked = b1;
				isBlock = b2;

				if(isBlock)
				{
					obj = c1;
				}

				break;
			case PASSWORD:
				password = s1;
				isBlock = b1;

				if(isBlock)
				{
					obj = c1;
				}

				break;
			case CLIENT_OPEN:
				guiType = i1;
				windowId = i2;
				isBlock = b1;

				if(isBlock)
				{
					obj = c1;
				}

				break;
			case SERVER_OPEN:
				useEnergy = b1;
				isBlock = b2;

				if(isBlock)
				{
					obj = c1;
				}

				break;
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, ByteBuf dataStream)
	{
		dataStream.writeInt(activeType.ordinal());

		switch(activeType)
		{
			case LOCK:
				dataStream.writeBoolean(locked);
				dataStream.writeBoolean(isBlock);

				if(isBlock)
				{
					dataStream.writeInt(obj.xCoord);
					dataStream.writeInt(obj.yCoord);
					dataStream.writeInt(obj.zCoord);
				}

				break;
			case PASSWORD:
				PacketHandler.writeString(dataStream, password);
				dataStream.writeBoolean(isBlock);

				if(isBlock)
				{
					dataStream.writeInt(obj.xCoord);
					dataStream.writeInt(obj.yCoord);
					dataStream.writeInt(obj.zCoord);
				}

				break;
			case CLIENT_OPEN:
				dataStream.writeInt(guiType);
				dataStream.writeInt(windowId);
				dataStream.writeBoolean(isBlock);

				if(isBlock)
				{
					dataStream.writeInt(obj.xCoord);
					dataStream.writeInt(obj.yCoord);
					dataStream.writeInt(obj.zCoord);
				}

				break;
			case SERVER_OPEN:
				dataStream.writeBoolean(useEnergy);
				dataStream.writeBoolean(isBlock);

				if(isBlock)
				{
					dataStream.writeInt(obj.xCoord);
					dataStream.writeInt(obj.yCoord);
					dataStream.writeInt(obj.zCoord);
				}

				break;
		}
	}

	@Override
	public void read(ChannelHandlerContext ctx, EntityPlayer player, ByteBuf dataStream)
	{
		ElectricChestPacketType packetType = ElectricChestPacketType.values()[dataStream.readInt()];

		if(packetType == ElectricChestPacketType.SERVER_OPEN)
		{
			try {
				boolean energy = dataStream.readBoolean();
				boolean block = dataStream.readBoolean();

				if(block)
				{
					int x = dataStream.readInt();
					int y = dataStream.readInt();
					int z = dataStream.readInt();

					TileEntityElectricChest tileEntity = (TileEntityElectricChest)player.worldObj.getTileEntity(x, y, z);

					if(energy)
					{
						tileEntity.setEnergy(tileEntity.getEnergy() - 100);
					}

					MekanismUtils.openElectricChestGui((EntityPlayerMP)player, tileEntity, null, true);
				}
				else {
					ItemStack stack = player.getCurrentEquippedItem();

					if(stack != null && stack.getItem() instanceof IElectricChest && MachineType.get(stack) == MachineType.ELECTRIC_CHEST)
					{
						if(energy)
						{
							((IEnergizedItem)stack.getItem()).setEnergy(stack, ((IEnergizedItem)stack.getItem()).getEnergy(stack) - 100);
						}

						InventoryElectricChest inventory = new InventoryElectricChest(player);
						MekanismUtils.openElectricChestGui((EntityPlayerMP)player, null, inventory, false);
					}
				}
			} catch(Exception e) {
				System.err.println("[Mekanism] Error while handling electric chest open packet.");
				e.printStackTrace();
			}
		}
		else if(packetType == ElectricChestPacketType.CLIENT_OPEN)
		{
			try {
				int type = dataStream.readInt();
				int id = dataStream.readInt();
				boolean block = dataStream.readBoolean();

				int x = 0;
				int y = 0;
				int z = 0;

				if(block)
				{
					x = dataStream.readInt();
					y = dataStream.readInt();
					z = dataStream.readInt();
				}

				Mekanism.proxy.openElectricChest(player, type, id, block, x, y, z);
			} catch(Exception e) {
				System.err.println("[Mekanism] Error while handling electric chest open packet.");
				e.printStackTrace();
			}
		}
		else if(packetType == ElectricChestPacketType.PASSWORD)
		{
			try {
				String pass = PacketHandler.readString(dataStream);
				boolean block = dataStream.readBoolean();

				if(block)
				{
					int x = dataStream.readInt();
					int y = dataStream.readInt();
					int z = dataStream.readInt();

					TileEntityElectricChest tileEntity = (TileEntityElectricChest)player.worldObj.getTileEntity(x, y, z);
					tileEntity.password = pass;
					tileEntity.authenticated = true;
				}
				else {
					ItemStack stack = player.getCurrentEquippedItem();

					if(stack != null && stack.getItem() instanceof IElectricChest && MachineType.get(stack) == MachineType.ELECTRIC_CHEST)
					{
						((IElectricChest)stack.getItem()).setPassword(stack, pass);
						((IElectricChest)stack.getItem()).setAuthenticated(stack, true);
					}
				}
			} catch(Exception e) {
				System.err.println("[Mekanism] Error while handling electric chest password packet.");
				e.printStackTrace();
			}
		}
		else if(packetType == ElectricChestPacketType.LOCK)
		{
			try {
				boolean lock = dataStream.readBoolean();
				boolean block = dataStream.readBoolean();

				if(block)
				{
					int x = dataStream.readInt();
					int y = dataStream.readInt();
					int z = dataStream.readInt();

					TileEntityElectricChest tileEntity = (TileEntityElectricChest)player.worldObj.getTileEntity(x, y, z);
					tileEntity.locked = lock;
				}
				else {
					ItemStack stack = player.getCurrentEquippedItem();

					if(stack != null && stack.getItem() instanceof IElectricChest && MachineType.get(stack) == MachineType.ELECTRIC_CHEST)
					{
						((IElectricChest)stack.getItem()).setLocked(stack, lock);
					}
				}
			} catch(Exception e) {
				System.err.println("[Mekanism] Error while handling electric chest password packet.");
				e.printStackTrace();
			}
		}
	}

	@Override
	public void handleClientSide(EntityPlayer player)
	{

	}

	@Override
	public void handleServerSide(EntityPlayer player)
	{

	}

	public static enum ElectricChestPacketType
	{
		LOCK,
		PASSWORD,
		CLIENT_OPEN,
		SERVER_OPEN
	}
}