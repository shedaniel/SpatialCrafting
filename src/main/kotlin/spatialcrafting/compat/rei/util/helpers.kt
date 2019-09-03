@file:Suppress("FunctionName")

package spatialcrafting.compat.rei.util

import me.shedaniel.rei.gui.widget.ButtonWidget
import me.shedaniel.rei.gui.widget.SlotWidget
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.text.LiteralText
import net.minecraft.text.Text


fun SlotWidget(x: Int, y: Int, itemStack: ItemStack, drawBackground: Boolean = true,
               showToolTips: Boolean = true, clickToMoreRecipes: Boolean = true,
               itemCountOverlay: (ItemStack) -> String = { "" }): SlotWidget {
    return SlotWidget(x, y, listOf(itemStack), drawBackground, showToolTips, clickToMoreRecipes, itemCountOverlay)
}

fun SlotWidget(x: Int, y: Int, itemStackList: List<ItemStack>, drawBackground: Boolean = true,
               showToolTips: Boolean = true, clickToMoreRecipes: Boolean = true,
               itemCountOverlay: (ItemStack) -> String = { "" }): SlotWidget {
    return object : SlotWidget(x, y, itemStackList, drawBackground, showToolTips, clickToMoreRecipes) {
        override fun getItemCountOverlay(currentStack: ItemStack): String = itemCountOverlay(currentStack)
    }
}
