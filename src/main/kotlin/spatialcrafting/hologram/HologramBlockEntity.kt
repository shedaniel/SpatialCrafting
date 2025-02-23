@file:UseSerializers(ForIngredient::class)

package spatialcrafting.hologram

import alexiil.mc.lib.attributes.AttributeList
import drawer.ForIngredient
import kotlinx.serialization.UseSerializers
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachmentBlockEntity
import net.fabricmc.fabric.api.server.PlayerStream
import net.minecraft.block.entity.BlockEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.recipe.Ingredient
import net.minecraft.util.Tickable
import spatialcrafting.Packets
import spatialcrafting.crafter.CrafterMultiblock
import spatialcrafting.crafter.CrafterPieceEntity
import spatialcrafting.util.*
import spatialcrafting.util.kotlinwrappers.Builders

private const val TicksPerSecond = 20

class HologramBlockEntity : BlockEntity(Type), BlockEntityClientSerializable, RenderAttachmentBlockEntity, Tickable {

    override fun getRenderAttachmentData(): Any = ghostIngredient?.let {
        if (it.matches(getItem())) ItemStack.EMPTY
        else it.stackArray[(ghostIngredientCycleIndex / TicksPerSecond) % it.stackArray.size]
    } ?: ItemStack.EMPTY


    companion object {
        val Type = Builders.blockEntityType(HologramBlock) { HologramBlockEntity() }

        private object Keys {
            const val Inventory = "inventory"
            const val LastChangeTime = "last_change_time"
        }
    }


    val inventory = HologramInventory().also {
        // Since inventory is only changed at server side we need to send a packet to the client
        // Note: this will be called again in the client after we do that, so we need to ignore it that time.
        it.setOwnerListener { _, _, previousStack, currentStack ->
            lastChangeTime = world!!.time
            if (world!!.isClient) {
                return@setOwnerListener
            }

            if (!previousStack.isItemEqual(currentStack)) {
                PlayerStream.watching(this).sendPacket(Packets.UpdateHologramContent(this.pos, currentStack))
            }
        }
    }


    private val ghostIngredient: Ingredient?
        get() = try {
            getMultiblock().hologramGhostIngredientFor(this).also { ghostIngredientActive = it != null }
        } catch (e: IllegalStateException) {
            logWarning { "Could not get ghost ingredient: $e" }
            null
        }

    /**
     * A way to avoid checking if there is a ghost ingredient every tick
     */
    private var ghostIngredientActive: Boolean = false

    override fun tick(/*client : MinecraftClient*/) {
        // Value is always incremented to avoid desyncs
        if (world?.isClient == true) {
            ghostIngredientCycleIndex++
            if (ghostIngredientActive) {
                if (ghostIngredientCycleIndex % TicksPerSecond == 0) Client.scheduleRenderUpdate(pos)
            }
        }


    }

    /**
     * Measured in ticks and then divided by 20. Used to change the item that is showed in the ghost item constantly
     */
    private var ghostIngredientCycleIndex = 0

    /**
     * Used for [spatialcrafting.client.particle.ItemMovementParticle] so it doesn't appear that the item arrives instantly when
     * in the particle it looks like it is still travelling
     */
    var contentsAreTravelling = false


    /**
     * This is just used for client sided rendering of the block so the items in the holograms don't move in sync.
     */
    var lastChangeTime: Long = 0

    override fun toTag(tag: CompoundTag): CompoundTag {
        super.toTag(tag)
        tag.put(Keys.Inventory, inventory.toTag())
        tag.putLong(Keys.LastChangeTime, lastChangeTime)
        return tag
    }

    override fun fromTag(tag: CompoundTag) {
        super.fromTag(tag)
        inventory.fromTag(tag.getCompound(Keys.Inventory))
        lastChangeTime = tag.getLong(Keys.LastChangeTime)
    }

    override fun toClientTag(p0: CompoundTag): CompoundTag = toTag(p0)

    override fun fromClientTag(p0: CompoundTag) = fromTag(p0)

    /**
     * Inserts only one of the itemStack
     */
    fun insertItem(itemStack: ItemStack) {
        val world = world!!
        markDirty()
        assert(isEmpty())
        lastChangeTime = world.time
        inventory.insert(itemStack.copy(count = 1))
        if (world.isServer) {
            val multiblock = getMultiblock()
            if (multiblock.recipeHelpActive) {
                multiblock.bumpRecipeHelpCurrentLayerIfNeeded(world)
            }
        }

    }

    fun getItem(): ItemStack = inventory.getInvStack(0)

    /**
     * May return an empty stack
     */
    fun extractItem(): ItemStack {
        val item = inventory.extract(1)
        markDirty()
        return item
    }

    fun isEmpty() = getItem().isEmpty

    fun registerInventory(to: AttributeList<*>) = inventory.offerSelfAsAttribute(to, null, null)

    // Note: we don't change recipe help here because this is only called when the entire multiblock is destroyed
    fun dropInventory() = world!!.dropItemStack(getItem(), pos)


    fun getMultiblock(): CrafterMultiblock {
        val world = world!!
        // We just go down until we find a crafter
        var currentPos = pos.down()
        while (true) {
            val entityBelow = world.getBlockEntity(currentPos)
            if (entityBelow !is HologramBlockEntity) {
                if (entityBelow is CrafterPieceEntity) {
                    return entityBelow.multiblockIn
                            ?: error("A hologram should always have a multiblock," +
                                    " and yet when looking at a crafter piece below at position $pos he did not have a multiblock instance.")
                }
                else {
                    error("Looked down below a hologram, and instead of finding a crafter entity, a $entityBelow was found!")
                }
            }
            currentPos = currentPos.down()
        }


    }

}

