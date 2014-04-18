package mekanism.client.gui;

import java.util.ArrayList;
import java.util.List;

import mekanism.api.Coord4D;
import mekanism.api.ListUtils;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.api.gas.OreGas;
import mekanism.client.gui.GuiEnergyInfo.IInfoHandler;
import mekanism.client.gui.GuiGasGauge.IGasInfoHandler;
import mekanism.client.gui.GuiProgress.IProgressInfoHandler;
import mekanism.client.gui.GuiProgress.ProgressBar;
import mekanism.client.gui.GuiSlot.SlotOverlay;
import mekanism.client.gui.GuiSlot.SlotType;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.PacketHandler;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.inventory.container.ContainerChemicalCrystallizer;
import mekanism.common.network.PacketTileEntity;
import mekanism.common.tile.TileEntityChemicalCrystallizer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiChemicalCrystallizer extends GuiMekanism
{
	public TileEntityChemicalCrystallizer tileEntity;

	public Gas prevGas;

	public ItemStack renderStack;

	public int stackSwitch = 0;

	public int stackIndex = 0;

	public List<ItemStack> iterStacks = new ArrayList<ItemStack>();

	public GuiChemicalCrystallizer(InventoryPlayer inventory, TileEntityChemicalCrystallizer tentity)
	{
		super(tentity, new ContainerChemicalCrystallizer(inventory, tentity));
		tileEntity = tentity;

		guiElements.add(new GuiRedstoneControl(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png")));
		guiElements.add(new GuiPowerBar(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png"), 160, 23));
		guiElements.add(new GuiConfigurationTab(this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png")));
		guiElements.add(new GuiEnergyInfo(new IInfoHandler() {
			@Override
			public List<String> getInfo()
			{
				String multiplier = MekanismUtils.getEnergyDisplay(tileEntity.ENERGY_USAGE);
				return ListUtils.asList("Using: " + multiplier + "/t", "Needed: " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy()-tileEntity.getEnergy()));
			}
		}, this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png")));
		guiElements.add(new GuiGasGauge(new IGasInfoHandler() {
			@Override
			public GasTank getTank()
			{
				return tileEntity.inputTank;
			}
		}, GuiGauge.Type.STANDARD, this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png"), 5, 4));
		guiElements.add(new GuiSlot(SlotType.EXTRA, this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png"), 5, 64).with(SlotOverlay.PLUS));
		guiElements.add(new GuiSlot(SlotType.POWER, this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png"), 154, 4).with(SlotOverlay.POWER));
		guiElements.add(new GuiSlot(SlotType.OUTPUT, this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png"), 130, 56));

		guiElements.add(new GuiProgress(new IProgressInfoHandler()
		{
			@Override
			public double getProgress()
			{
				return tileEntity.getScaledProgress();
			}
		}, ProgressBar.LARGE_RIGHT, this, tileEntity, MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png"), 51, 60));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);

		fontRenderer.drawString(tileEntity.getInvName(), 37, 4, 0x404040);

		if(tileEntity.inputTank.getGas() != null)
		{
			fontRenderer.drawString(tileEntity.inputTank.getGas().getGas().getLocalizedName(), 29, 15, 0x00CD00);

			if(tileEntity.inputTank.getGas().getGas() instanceof OreGas)
			{
				fontRenderer.drawString("(" + ((OreGas)tileEntity.inputTank.getGas().getGas()).getOreName() + ")", 29, 24, 0x00CD00);
			}
		}

		if(renderStack != null)
		{
			try {
				GL11.glPushMatrix();
				GL11.glEnable(GL11.GL_LIGHTING);
				itemRenderer.renderItemAndEffectIntoGUI(fontRenderer, mc.getTextureManager(), renderStack, 131, 14);
				GL11.glDisable(GL11.GL_LIGHTING);
				GL11.glPopMatrix();
			} catch(Exception e) {}
		}

		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
	{
		mc.renderEngine.bindTexture(MekanismUtils.getResource(ResourceType.GUI, "GuiChemicalCrystallizer.png"));
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;
		drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);

		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);
	}

	private Gas getInputGas()
	{
		return tileEntity.inputTank.getGas() != null ? tileEntity.inputTank.getGas().getGas() : null;
	}

	private void resetStacks()
	{
		iterStacks.clear();
		renderStack = null;
		stackSwitch = 0;
		stackIndex = -1;
	}

	@Override
	protected void mouseClicked(int x, int y, int button)
	{
		super.mouseClicked(x, y, button);

		if(button == 0)
		{
			int xAxis = (x - (width - xSize) / 2);
			int yAxis = (y - (height - ySize) / 2);

			if(xAxis > 24 && xAxis < 42 && yAxis > 56 && yAxis < 64)
			{
				ArrayList data = new ArrayList();
				data.add(0);

				PacketHandler.sendPacket(Transmission.SERVER, new PacketTileEntity().setParams(Coord4D.get(tileEntity), data));
				mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
			}
		}
	}

	@Override
	public void updateScreen()
	{
		super.updateScreen();

		if(prevGas != getInputGas())
		{
			prevGas = getInputGas();

			boolean reset = false;

			if(prevGas == null || !(prevGas instanceof OreGas) || !((OreGas)prevGas).isClean())
			{
				reset = true;
				resetStacks();
			}

			if(!reset)
			{
				OreGas gas = (OreGas)prevGas;
				String oreDictName = "ore" + gas.getName().substring(5);

				updateStackList(oreDictName);
			}
		}

		if(stackSwitch > 0)
		{
			stackSwitch--;
		}

		if(stackSwitch == 0 && iterStacks != null && iterStacks.size() > 0)
		{
			stackSwitch = 20;

			if(stackIndex == -1 || stackIndex == iterStacks.size()-1)
			{
				stackIndex = 0;
			}
			else if(stackIndex < iterStacks.size()-1)
			{
				stackIndex++;
			}

			renderStack = iterStacks.get(stackIndex);
		}
		else if(iterStacks != null && iterStacks.size() == 0)
		{
			renderStack = null;
		}
	}

	private void updateStackList(String oreName)
	{
		if(iterStacks == null)
		{
			iterStacks = new ArrayList<ItemStack>();
		}
		else {
			iterStacks.clear();
		}

		List<String> keys = new ArrayList<String>();

		for(String s : OreDictionary.getOreNames())
		{
			if(oreName.equals(s) || oreName.equals("*"))
			{
				keys.add(s);
			}
			else if(oreName.endsWith("*") && !oreName.startsWith("*"))
			{
				if(s.startsWith(oreName.substring(0, oreName.length()-1)))
				{
					keys.add(s);
				}
			}
			else if(oreName.startsWith("*") && !oreName.endsWith("*"))
			{
				if(s.endsWith(oreName.substring(1)))
				{
					keys.add(s);
				}
			}
			else if(oreName.startsWith("*") && oreName.endsWith("*"))
			{
				if(s.contains(oreName.substring(1, oreName.length()-1)))
				{
					keys.add(s);
				}
			}
		}

		for(String key : keys)
		{
			for(ItemStack stack : OreDictionary.getOres(key))
			{
				ItemStack toAdd = stack.copy();

				if(!iterStacks.contains(stack) && toAdd.getItem() instanceof ItemBlock)
				{
					iterStacks.add(stack.copy());
				}
			}
		}

		stackSwitch = 0;
		stackIndex = -1;
	}
}
