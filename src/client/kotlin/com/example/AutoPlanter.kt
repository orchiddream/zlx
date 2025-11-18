package com.example

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.core.Direction

import kotlin.random.Random

/**
 * 自动种花器
 * 功能：每隔一定 tick 在玩家附近尝试种植一朵花，花来源固定为物品栏第一格。
 * 限制：仅支持单格高度的花类，双高花不在当前实现范围内。
 * 条件：上方必须是空气，地面必须为草方块/泥土/粗泥土/灰化土，且花方块能够在该位置存活。
 */
class AutoPlanter {
    companion object {
        /**
         * 可种植的花物品清单（单格花）。
         * 仅保留常见单格花，避免双高花或非花类造成放置失败。
         */
        private val PLANTABLE_ITEMS = listOf(
            Items.DANDELION,
            Items.POPPY,
            Items.BLUE_ORCHID,
            Items.ALLIUM,
            Items.AZURE_BLUET,
            Items.RED_TULIP,
            Items.ORANGE_TULIP,
            Items.WHITE_TULIP,
            Items.PINK_TULIP,
            Items.OXEYE_DAISY,
            Items.CORNFLOWER,
            Items.LILY_OF_THE_VALLEY,
            Items.WITHER_ROSE
        )

        /** 客户端 tick 计数器与节流（每 20 tick 执行一次） */
        private var tickCounter = 0
        private const val TICK_DELAY = 1
        
        // 添加用于检测玩家移动的变量
        private var lastPlayerPosition: Vec3? = null
        private var isPlayerMoving = false

        /**
         * 注册客户端 tick 事件：当开关开启时，按节流触发自动种花逻辑。
         */
        fun initialize() {
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                val mc = client
                val player = mc.player
                val level = mc.level
                if (!ZlxClient.isPlantingEnabled || player == null || level == null) {
                    // 重置位置记录
                    lastPlayerPosition = null
                    return@register
                }

                // 检查玩家是否在移动
                checkPlayerMovement(player.position())

                // 只有在玩家移动时才执行种植逻辑
                if (isPlayerMoving) {
                    tickCounter++
                    if (tickCounter >= TICK_DELAY) {
                        tickCounter = 0
                        performPlanting()
                    }
                }
            }
        }

        /**
         * 检查玩家是否在移动
         */
        private fun checkPlayerMovement(currentPosition: Vec3) {
            lastPlayerPosition?.let { lastPos ->
                // 计算位置差异
                val deltaX = currentPosition.x - lastPos.x
                val deltaY = currentPosition.y - lastPos.y
                val deltaZ = currentPosition.z - lastPos.z
                
                // 如果位置发生变化，则认为玩家在移动
                // 使用一个小的阈值来避免浮点数精度问题
                val threshold = 0.001
                isPlayerMoving = Math.abs(deltaX) > threshold || 
                               Math.abs(deltaY) > threshold || 
                               Math.abs(deltaZ) > threshold
            }
            
            // 更新最后位置
            lastPlayerPosition = currentPosition
        }

        /**
         * 主逻辑：以玩家为中心，随机扫描邻域，若第一格为花则尝试放置一朵。
         */
        private fun performPlanting() {
            val mc = Minecraft.getInstance()
            val player = mc.player ?: return
            val level = mc.level ?: return

            val playerPos = player.blockPosition()

            val radius = 4
            val positionsToCheck = mutableListOf<BlockPos>()

            for (x in -radius..radius) {
                for (z in -radius..radius) {
                    // 检测玩家脚下同一高度的地面（y = -1）
                    val y = -1
                    if (x == 0 && z == 0) continue
                    val pos = playerPos.offset(x, y, z)
                    positionsToCheck.add(pos)
                }
            }

            positionsToCheck.shuffle()

            val firstSlotStack = findFirstSlotFlower(player.inventory) ?: return

            for (pos in positionsToCheck) {
                if (tryPlantAt(level, pos, firstSlotStack)) {
                    break
                }
            }
        }

        /**
         * 仅获取物品栏第一格的花。
         * 若第一格为空或非花，则返回 null。
         */
        private fun findFirstSlotFlower(inventory: Inventory): ItemStack? {
            val stack = inventory.getItem(0)
            if (stack.isEmpty) return null
            return if (PLANTABLE_ITEMS.contains(stack.item)) stack else null
        }

        /**
         * 放置判定与执行：
         * - 上方必须为空气
         * - 地面为合适方块
         * - 该花方块能在目标位置存活
         * 满足条件则在目标位置上方放置花。
         */
        private fun tryPlantAt(level: Level, pos: BlockPos, stack: ItemStack): Boolean {
            val groundState = level.getBlockState(pos)
            val aboveState = level.getBlockState(pos.above())

            if (!aboveState.isAir()) return false
            if (!isSuitableGround(groundState.block)) return false

            val block = Block.byItem(stack.item)
            val targetPos = pos.above()
            val state = block.defaultBlockState()
            if (!state.canSurvive(level, targetPos)) return false

            val mc = Minecraft.getInstance()
            val player = mc.player ?: return false
            if (player.mainHandItem.item != stack.item) return false
            val hit = BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
            val result = mc.gameMode?.useItemOn(player, InteractionHand.MAIN_HAND, hit)
            return result != null && result.consumesAction()
        }

        /** 合适的地面判定（草方块/泥土/粗泥土/灰化土） */
        private fun isSuitableGround(block: Block): Boolean {
            return block === Blocks.GRASS_BLOCK ||
                   block === Blocks.DIRT ||
                   block === Blocks.COARSE_DIRT ||
                   block === Blocks.PODZOL
        }
    }
}