package net.teamfruit.rerecipecreators.serialization;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import com.google.common.collect.Maps;

import net.teamfruit.rerecipecreators.ReRecipeCreators;
import net.teamfruit.rerecipecreators.Recipes;

public class RecipeStorage {
	private final FilenameFilter recipefilefilter = new FilenameFilter() {
		@Override
		public boolean accept(final File dir, final String name) {
			return StringUtils.endsWith(name, ".json");
		}
	};

	private final File recipeDir;
	private final Map<String, SerializedRecipe> recipes = Maps.newHashMap();

	public RecipeStorage(final File recipeDir) {
		this.recipeDir = recipeDir;
	}

	public RecipeStorage load() {
		this.recipes.clear();
		for (final File recipeFile : this.recipeDir.listFiles(this.recipefilefilter)) {
			final SerializedRecipe recipe = loadRecipe(recipeFile);
			if (recipe!=null)
				this.recipes.put(fromLocation(recipeFile), recipe);
		}
		return this;
	}

	public Map<String, SerializedRecipe> getRecipes() {
		return Collections.unmodifiableMap(this.recipes);
	}

	public boolean putRecipe(final String name, final SerializedRecipe recipe) {
		if (saveRecipe(toLocation(name), recipe)) {
			this.recipes.put(name, recipe);
			return true;
		}
		return false;
	}

	public boolean removeRecipe(final String name) {
		if (deleteRecipe(toLocation(name))) {
			this.recipes.remove(name);
			return true;
		}
		return false;
	}

	public SerializedRecipe getRecipe(final String name) {
		return this.recipes.get(name);
	}

	public String getIDFromResult(final ItemStack result) {
		for (final Entry<String, SerializedRecipe> entry : this.recipes.entrySet()) {
			final SerializedRecipe sRecipe = entry.getValue();
			if (sRecipe!=null) {
				final Recipe recipe = sRecipe.getRecipe();
				if (recipe!=null&&recipe.getResult().isSimilar(result))
					return entry.getKey();
			}
		}
		return null;
	}

	public String getIDFromRecipe(final Recipe recipe) {
		for (final Entry<String, SerializedRecipe> entry : this.recipes.entrySet()) {
			final SerializedRecipe sRecipe = entry.getValue();
			if (sRecipe!=null&&Recipes.recipeEquals(recipe, sRecipe.getRecipe()))
				return entry.getKey();
		}
		return null;
	}

	public Pair<String, SerializedRecipe> getFromIngredients(final Recipe recipe) {
		for (final Entry<String, SerializedRecipe> entry : this.recipes.entrySet()) {
			final SerializedRecipe sRecipe = entry.getValue();
			if (sRecipe!=null&&Recipes.ingredientEquals(recipe, sRecipe.getRecipe()))
				return Pair.of(entry.getKey(), entry.getValue());
		}
		return null;
	}

	public Pair<String, SerializedRecipe> getFromIngredients(final CraftingInventory recipe) {
		for (final Entry<String, SerializedRecipe> entry : this.recipes.entrySet()) {
			final SerializedRecipe sRecipe = entry.getValue();
			if (sRecipe!=null&&Recipes.ingredientEquals(sRecipe.getRecipe(), recipe))
				return Pair.of(entry.getKey(), entry.getValue());
		}
		return null;
	}

	private File toLocation(final String name) {
		return new File(this.recipeDir, name+".json");
	}

	private String fromLocation(final File file) {
		return StringUtils.substringBeforeLast(file.getName(), ".json");
	}

	public static boolean saveRecipe(final File file, final SerializedRecipe recipe) {
		try {
			ObjectHandler.write(file, SerializedRecipe.class, recipe);
			return true;
		} catch (final Exception e) {
			ReRecipeCreators.instance.log.log(Level.WARNING, "Could not save recipe file '"+file.getName()+"': ", e);
		}
		return false;
	}

	public static boolean deleteRecipe(final File file) {
		return FileUtils.deleteQuietly(file);
	}

	public static SerializedRecipe loadRecipe(final File file) {
		try {
			return ObjectHandler.read(file, SerializedRecipe.class);
		} catch (final Exception e) {
			ReRecipeCreators.instance.log.log(Level.WARNING, "Could not load recipe file '"+file.getName()+"': ", e);
		}
		return null;
	}

	public static RecipeStorage createRecipesStorage(final File dataDir) {
		if (!dataDir.exists())
			dataDir.mkdir();
		final File recipeDir = new File(dataDir, "recipes");
		if (!recipeDir.exists())
			recipeDir.mkdir();
		return new RecipeStorage(recipeDir).load();
	}
}
