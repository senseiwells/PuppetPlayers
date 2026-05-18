package me.senseiwells.puppet.action

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import me.senseiwells.puppet.PuppetPlayer
import me.senseiwells.puppet.extensions.PlayerActionsExtension.Companion.popActionResult
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerGamePacketListener
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action
import net.minecraft.network.protocol.game.ServerboundSpectateEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.sqrt

class PlayerActions(
    private val player: ServerPlayer
) {
    private val chained = ArrayList<PlayerAction>()
    private val actions = ArrayList<PlayerAction>()
    private var action = 0

    private var destroyBlockPos = BlockPos(-1, -1, -1)
    private var destroyingItem = ItemStack.EMPTY
    private var destroyProgress = 0.0F
    private var destroyTicks = 0.0F
    private var destroyDelay = 0.0F
    private var isDestroying = false

    private var rightClickDelay = 0
    private var missTime = 0

    private lateinit var hitResult: HitResult

    var loop = false
    var paused = false

    var attacking: Boolean = false
    var attackingHeld: Boolean = false

    var using: Boolean = false
    var usingHeld: Boolean = false

    var jumping: Boolean = false

    fun run(action: PlayerAction): Boolean {
        if (!this.valid(action)) {
            return false
        }

        if (action.immediate) {
            action.run(this.player)
        } else {
            this.actions.add(action)
        }
        return true
    }

    fun chain(action: PlayerAction): Boolean {
        if (!this.valid(action)) {
            return false
        }

        this.chained.add(action)
        return true
    }

    fun remove(predicate: (PlayerAction) -> Boolean) {
        this.actions.removeIf(predicate)
        this.chained.removeIf(predicate)
    }

    fun restart() {
        this.action = 0
    }

    fun clear() {
        this.chained.clear()
    }

    fun valid(action: PlayerAction): Boolean {
        return action !is PuppetPlayerAction || this.isPuppet()
    }

    internal fun tick() {
        this.attacking = this.attackingHeld
        this.using = this.usingHeld

        this.hitResult = this.getHitResult()

        if (this.rightClickDelay > 0) {
            this.rightClickDelay--
        }
        if (this.missTime > 0) {
            this.missTime--
        }

        if (!this.paused) {
            this.runActions()
        }

        var attacked = false
        if (this.player.isUsingItem) {
            if (!this.using && this.isPuppet()) {
                this.releaseUsingItem()
            }
        } else {
            if (this.attacking) {
                attacked = this.startAttack()
            }
            if (this.using) {
                this.startUseItem()
            }
        }

        // Consume actions
        this.attacking = false
        this.using = false

        if (this.usingHeld && this.rightClickDelay == 0 && !this.player.isUsingItem) {
            this.startUseItem()
        }

        this.continueAttack(!attacked && this.attackingHeld)

        if (this.jumping) {
            this.jump()
        }
    }

    internal fun jump() {
        if (this.player is PuppetPlayer) {
            this.player.moveControl.jump()
        }
    }

    private fun isPuppet(): Boolean {
        return this.player is PuppetPlayer
    }

    private fun runActions() {
        val iter = this.actions.iterator()
        for (action in iter) {
            if (action.run(this.player) == PlayerAction.Result.Complete) {
                iter.remove()
            }
        }

        if (this.loop && this.action >= this.chained.size) {
            this.action = 0
        }
        val start = this.action
        while (true) {
            val action = this.chained.getOrNull(this.action) ?: break
            if (action.run(this.player) == PlayerAction.Result.Incomplete) {
                break
            }
            this.action += 1
            if (this.action >= this.chained.size) {
                if (!this.loop) {
                    break
                }
                this.action = 0
            }
            if (this.action == start) {
                break
            }
        }
    }

    private fun startAttack(): Boolean {
        if (this.missTime > 0) {
            return false
        }

        if (this.player.isSpectator) {
            val hitResult = this.hitResult
            if (hitResult is EntityHitResult) {
                this.handle(ServerboundSpectateEntityPacket(hitResult.entity.id))
                return true
            }
        }

        val heldItem = this.player.getItemInHand(InteractionHand.MAIN_HAND)
        if (!heldItem.isItemEnabled(this.player.level().enabledFeatures())) {
            return false
        }
        if (this.player.cannotAttackWithItem(heldItem, 0)) {
            return false
        }

        val piercingWeapon = heldItem.get(DataComponents.PIERCING_WEAPON)
        if (piercingWeapon != null) {
            this.piercingAttack()
            this.swing(InteractionHand.MAIN_HAND)
            return true
        }

        var endAttack = false
        when (val hitResult = this.hitResult) {
            is EntityHitResult -> {
                val range = heldItem.get(DataComponents.ATTACK_RANGE)
                if (range == null || range.isInRange(this.player, hitResult.location)) {
                    this.handle(ServerboundAttackPacket(hitResult.entity.id))
                }
            }
            is BlockHitResult -> {
                val pos = hitResult.blockPos
                if (!this.player.level().getBlockState(pos).isAir) {
                    this.startDestroyBlock(pos, hitResult.direction)
                    if (this.player.level().getBlockState(pos).isAir) {
                        endAttack = true
                    }
                }
            }
            else -> {
                if (!this.player.isCreative) {
                    this.missTime = 10
                }
            }
        }
        this.swing(InteractionHand.MAIN_HAND)
        return endAttack
    }

    private fun continueAttack(isLeftClick: Boolean) {
        if (!isLeftClick) {
            this.missTime = 0
        }
        if (this.missTime > 0 || this.player.isUsingItem) {
            return
        }
        val hitResult = this.hitResult
        if (isLeftClick && hitResult is BlockHitResult) {
            val blockPos = hitResult.blockPos
            if (!this.player.level().getBlockState(blockPos).isAir) {
                if (this.continueDestroyBlock(blockPos, hitResult.direction)) {
                    this.swing(InteractionHand.MAIN_HAND)
                }
            }
        } else {
            this.stopDestroyBlock()
        }
    }

    private fun piercingAttack() {
        this.handle(ServerboundPlayerActionPacket(Action.STAB, BlockPos.ZERO, Direction.DOWN))
    }

    private fun startUseItem() {
        if (this.isDestroying) {
            return
        }

        this.rightClickDelay = 4
        for (hand in InteractionHand.entries) {
            val stack = this.player.getItemInHand(hand)
            when (val hitResult = this.hitResult) {
                is EntityHitResult -> {
                    if (this.player.isWithinEntityInteractionRange(hitResult.entity, 0.0)) {
                        val result = this.interact(hitResult.entity, hitResult, hand)
                        if (result is InteractionResult.Success) {
                            if (result.swingSource == InteractionResult.SwingSource.SERVER) {
                                this.swing(hand)
                            }
                            return
                        }
                    }
                }
                is BlockHitResult ->  {
                    val result = this.useItemOn(hand, hitResult)
                    if (result is InteractionResult.Success) {
                        if (result.swingSource == InteractionResult.SwingSource.SERVER) {
                            this.swing(hand)
                        }
                        return
                    } else if (result is InteractionResult.Fail) {
                        return
                    }
                }
            }
            if (!stack.isEmpty) {
                val result = this.useItem(hand)
                if (result is InteractionResult.Success) {
                    if (result.swingSource == InteractionResult.SwingSource.SERVER) {
                        this.swing(hand)
                    }
                    return
                }
            }
        }
    }

    private fun swing(hand: InteractionHand) {
        this.player.swing(hand, true)
    }

    private fun releaseUsingItem() {
        this.handle(ServerboundPlayerActionPacket(Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN))
    }

    private fun startDestroyBlock(pos: BlockPos, face: Direction): Boolean {
        if (this.player.blockActionRestricted(this.player.level(), pos, this.player.gameMode.gameModeForPlayer)) {
            return false
        }
        if (!this.player.level().worldBorder.isWithinBounds(pos)) {
            return false
        }

        if (this.player.isCreative) {
            this.handle(ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, face))
            this.destroyDelay = 5.0F
            return true
        }
        if (!this.isDestroying || !this.sameDestroyTarget(pos)) {
            if (this.isDestroying) {
                this.handle(ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, face))
            }
            val state = this.player.level().getBlockState(pos)
            if (state.isAir || state.getDestroyProgress(this.player, this.player.level(), pos) < 1.0F) {
                this.isDestroying = true
                this.destroyBlockPos = pos
                this.destroyingItem = this.player.mainHandItem
                this.destroyProgress = 0.0F
                this.destroyTicks = 0.0F
            }

            this.handle(ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, face))
        }
        return true
    }

    private fun continueDestroyBlock(pos: BlockPos, face: Direction): Boolean {
        if (this.destroyDelay > 0) {
            this.destroyDelay--
            return true
        }
        if (this.player.isCreative && this.player.level().worldBorder.isWithinBounds(pos)) {
            this.handle(ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, pos, face))
            this.destroyDelay = 5.0F
            return true
        }
        if (!this.sameDestroyTarget(pos)) {
            return this.startDestroyBlock(pos, face)
        }
        val state = this.player.level().getBlockState(pos)
        if (state.isAir) {
            this.isDestroying = false
            return false
        }
        this.destroyProgress += state.getDestroyProgress(this.player, this.player.level(), pos)
        this.destroyTicks++
        if (this.destroyProgress >= 1.0F) {
            this.isDestroying = false
            this.handle(ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, pos, face))
            this.destroyProgress = 0.0F
            this.destroyTicks = 0.0F
            this.destroyDelay = 5.0F
        }
        return true
    }

    private fun stopDestroyBlock() {
        if (this.isDestroying) {
            this.handle(ServerboundPlayerActionPacket(Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN))
            this.isDestroying = false
            this.destroyProgress = 0.0f
        }
    }

    private fun interact(target: Entity, hitResult: EntityHitResult, hand: InteractionHand): InteractionResult {
        val delta = hitResult.location.subtract(target.x, target.y, target.z)
        this.handle(ServerboundInteractPacket(target.id, hand, delta, this.player.isShiftKeyDown))
        return this.player.popActionResult(InteractionResult.FAIL)
    }

    private fun useItemOn(hand: InteractionHand, result: BlockHitResult): InteractionResult {
        if (!this.player.level().worldBorder.isWithinBounds(result.blockPos)) {
            return InteractionResult.FAIL
        }
        this.handle(ServerboundUseItemOnPacket(hand, result, 0))
        return this.player.popActionResult(InteractionResult.PASS)
    }

    private fun useItem(hand: InteractionHand): InteractionResult {
        this.handle(ServerboundUseItemPacket(hand, 0, this.player.yRot, this.player.xRot))
        return this.player.popActionResult(InteractionResult.FAIL)
    }

    private fun getHitResult(): HitResult {
        val blockRange = this.player.blockInteractionRange()
        val entityRange = this.player.entityInteractionRange()
        var range = max(blockRange, entityRange)
        var rangeSqr = range * range
        val hitResult = this.player.pick(range, 1.0F, false)
        val eyes = this.player.eyePosition
        val distanceSqr = hitResult.location.distanceToSqr(eyes)

        if (hitResult.type != HitResult.Type.MISS) {
            rangeSqr = distanceSqr
            range = sqrt(distanceSqr)
        }

        val view = this.player.lookAngle
        val position = eyes.add(view.x * range, view.y * range, view.z * range)

        val box = this.player.boundingBox.expandTowards(view.scale(range)).inflate(1.0)
        val predicate = { entity: Entity -> !entity.isSpectator && entity.isPickable }
        val entityHitResult = ProjectileUtil.getEntityHitResult(this.player, eyes, position, box, predicate, rangeSqr)
        if (entityHitResult != null && entityHitResult.location.distanceToSqr(eyes) < distanceSqr) {
            return this.filterHitResult(entityHitResult, eyes, entityRange)
        }
        return this.filterHitResult(hitResult, eyes, blockRange)
    }

    private fun filterHitResult(hitResult: HitResult, pos: Vec3, range: Double): HitResult {
        val vec3 = hitResult.location
        if (!vec3.closerThan(pos, range)) {
            val vec32 = hitResult.location
            val direction = Direction.getApproximateNearest(vec32.x - pos.x, vec32.y - pos.y, vec32.z - pos.z)
            return BlockHitResult.miss(vec32, direction, BlockPos.containing(vec32))
        } else {
            return hitResult
        }
    }

    private fun sameDestroyTarget(pos: BlockPos): Boolean {
        val destroyingItem = this.player.mainHandItem
        return pos == this.destroyBlockPos && ItemStack.isSameItemSameComponents(destroyingItem, this.destroyingItem)
    }

    private fun <T: Packet<ServerGamePacketListener>> handle(packet: T) {
        packet.handle(this.player.connection)
    }

    internal fun pack(): Packed {
        return Packed(
            this.attacking,
            this.using,
            this.attackingHeld,
            this.usingHeld,
            this.loop,
            this.action,
            this.chained
        )
    }

    internal fun unpack(packed: Packed) {
        this.attacking = packed.attacking
        this.using = packed.using
        this.attackingHeld = packed.attackingHeld
        this.usingHeld = packed.usingHeld
        this.loop = packed.loop
        this.action = packed.action
        this.chained.addAll(packed.actions)
    }

    data class Packed(
        val attacking: Boolean,
        val using: Boolean,
        val attackingHeld: Boolean,
        val usingHeld: Boolean,
        val loop: Boolean,
        val action: Int,
        val actions: List<PlayerAction>
    ) {
        companion object {
            val CODEC: Codec<Packed> = RecordCodecBuilder.create { instance ->
                instance.group(
                    Codec.BOOL.fieldOf("attacking").forGetter(Packed::attacking),
                    Codec.BOOL.fieldOf("using").forGetter(Packed::using),
                    Codec.BOOL.fieldOf("attacking_held").forGetter(Packed::attackingHeld),
                    Codec.BOOL.fieldOf("using_held").forGetter(Packed::usingHeld),
                    Codec.BOOL.fieldOf("loop").forGetter(Packed::loop),
                    Codec.INT.fieldOf("action").forGetter(Packed::action),
                    PlayerAction.CODEC.listOf().optionalFieldOf("actions", listOf()).forGetter(Packed::actions)
                ).apply(instance, ::Packed)
            }
        }
    }
}