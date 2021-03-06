package minechem.tileentity.decomposer;

import java.util.*;

import cpw.mods.fml.common.registry.GameRegistry;
import minechem.Settings;
import minechem.api.IDecomposerControl;
import minechem.potion.PotionChemical;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

public class DecomposerRecipe
{
	public static Map<String, DecomposerRecipe> recipes = new LinkedHashMap<String, DecomposerRecipe>();

	private static final Random rand = new Random();
	ItemStack input;
	public Map<PotionChemical, PotionChemical> output = new LinkedHashMap<PotionChemical, PotionChemical>();

	//TODO:Add blacklist support for fluids
	public static DecomposerRecipe add(DecomposerRecipe recipe)
	{
		if (recipe.input != null && recipe.input.getItem() != null)
		{
			if (isBlacklisted(recipe.input) || (recipe.input.getItem() instanceof IDecomposerControl && ((IDecomposerControl)recipe.input.getItem()).getDecomposerMultiplier(recipe.input)==0))
			{
				return null;
			}
			recipes.put(getKey(recipe.input), recipe);
		} else if (recipe instanceof DecomposerFluidRecipe && ((DecomposerFluidRecipe) recipe).inputFluid != null)
		{
			recipes.put(getKey(((DecomposerFluidRecipe) recipe).inputFluid), recipe);
		}
		return recipe;
	}

	public static DecomposerRecipe get(String string)
	{
		return recipes.get(string);
	}

	public static DecomposerRecipe remove(String string)
	{
		if (recipes.containsKey(string))
		{
			return recipes.remove(string);
		}
		return null;
	}

	public static DecomposerRecipe remove(ItemStack itemStack)
	{
		if (recipes.containsKey(getKey(itemStack)))
		{
			return recipes.remove(getKey(itemStack));
		}
		return null;
	}

	public static String getKey(ItemStack itemStack)
	{
		if (itemStack != null && itemStack.getItem() != null)
		{
			GameRegistry.UniqueIdentifier key = GameRegistry.findUniqueIdentifierFor(itemStack.getItem());
			if (key!=null) return key.toString()+ "@" + itemStack.getItemDamage();
			return itemStack.getItem().getUnlocalizedName(itemStack) +"@" + itemStack.getItemDamage();
		}
		return null;
	}

	public static String getKey(FluidStack fluidStack)
	{
		if (fluidStack != null && fluidStack.getFluid() != null)
		{
			FluidStack result = fluidStack.copy();
			result.amount = 1;
			return result.toString();
		}
		return null;
	}

	public static DecomposerRecipe get(ItemStack item)
	{
		String key = getKey(item);
		if (key != null)
		{
			return get(key);
		}
		return null;
	}

	public static DecomposerRecipe get(FluidStack item)
	{
		String key = getKey(item);
		if (key != null)
		{
			return get(key);
		}
		return null;
	}

	public static void removeRecipeSafely(String item)
	{
		for (ItemStack i : OreDictionary.getOres(item))
		{
			DecomposerRecipe.remove(i);
		}
	}

	public static void createAndAddRecipeSafely(String item, PotionChemical... chemicals)
	{
		for (ItemStack i : OreDictionary.getOres(item))
		{
			DecomposerRecipe.add(new DecomposerRecipe(new ItemStack(i.getItem(), 1, i.getItemDamage()), chemicals));
		}
	}

	public DecomposerRecipe(ItemStack input, PotionChemical... chemicals)
	{
		this(chemicals);
		this.input = input;
	}

	public DecomposerRecipe(ItemStack input, ArrayList<PotionChemical> chemicals)
	{
		this.input = input;
		for (PotionChemical potionChemical : chemicals)
		{
			PotionChemical current = this.output.put(getPotionKey(potionChemical), potionChemical);
			if (current != null)
			{
				current.amount += potionChemical.amount;
				this.output.put(getPotionKey(potionChemical), potionChemical);
			}
		}
	}

	public DecomposerRecipe(PotionChemical... chemicals)
	{
		addChemicals(chemicals);
	}

	public void addChemicals(PotionChemical... chemicals)
	{
		for (PotionChemical potionChemical : chemicals)
		{
			PotionChemical current = this.output.get(getPotionKey(potionChemical));
			if (current != null)
			{
				current.amount += potionChemical.amount;
				continue;
			}
			this.output.put(getPotionKey(potionChemical), potionChemical);
		}
	}

	public ItemStack getInput()
	{
		return this.input;
	}

	public ArrayList<PotionChemical> getOutput()
	{
		ArrayList<PotionChemical> result = new ArrayList<PotionChemical>();
		result.addAll(this.output.values());
		return result;
	}

	public static PotionChemical getPotionKey(PotionChemical potion)
	{
		PotionChemical key = potion.copy();
		key.amount = 1;
		return key;
	}

	public ArrayList<PotionChemical> getOutputRaw()
	{
		ArrayList<PotionChemical> result = new ArrayList<PotionChemical>();
		result.addAll(this.output.values());
		return result;
	}

	public ArrayList<PotionChemical> getPartialOutputRaw(int f)
	{
		ArrayList<PotionChemical> raw = getOutput();
		ArrayList<PotionChemical> result = new ArrayList<PotionChemical>();
		if (raw != null)
		{
			for (PotionChemical chem : raw)
			{
				try
				{
					if (chem != null)
					{
						PotionChemical reduced = chem.copy();
						if (reduced != null)
						{
							reduced.amount = (int) Math.floor(chem.amount / f);
							if (reduced.amount == 0 && rand.nextFloat() > (chem.amount / f))
							{
								reduced.amount = 1;
							}
							result.add(reduced);
						}
					}

				} catch (Exception e)
				{
					// something has gone wrong
					// but we do not know quite why
					// debug code goes here
				}

			}
		}

		return result;
	}

	public boolean isNull()
	{
		return this.output == null;
	}

	public boolean hasOutput()
	{
		return !this.output.values().isEmpty();
	}

	public boolean outputContains(PotionChemical potionChemical)
	{
		boolean contains = false;
		for (PotionChemical output : this.output.values())
		{
			contains = potionChemical.sameAs(output);
			if (contains)
			{
				break;
			}
		}
		return contains;
	}

	public static boolean isBlacklisted(ItemStack itemStack)
	{
		for (ItemStack stack:Settings.decomposerBlacklist)
			if (stack.getItem()==itemStack.getItem()&&(stack.getItemDamage()==Short.MAX_VALUE||stack.getItemDamage()==itemStack.getItemDamage())) return true;
		return false;
	}

    public float getChance()
    {
        return 1.0F;
    }
}
