package com.example

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.lwjgl.glfw.GLFW

/**
 * 客户端入口与开关控制
 * - 默认按键 P 切换“自动种花”开关
 * - 屏幕顶部居中显示当前开关状态，持续约 5 秒
 */
object ZlxClient : ClientModInitializer {
    lateinit var toggleKey: KeyMapping
        private set

    var isPlantingEnabled = false
        private set

    private var hudVisibleTicks = 0

    override fun onInitializeClient() {
        // 初始化自动种花器（注册 tick 事件与主逻辑）
        AutoPlanter.initialize()

        // 注册按键绑定：按下 P 切换开关
        toggleKey = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.zlx.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.zlx.title"
            )
        )

        // 轮询按键状态并更新 HUD 显示计时器
        ClientTickEvents.END_CLIENT_TICK.register { _ ->
            while (toggleKey.consumeClick()) {
                isPlantingEnabled = !isPlantingEnabled
                hudVisibleTicks = 100
            }

            if (hudVisibleTicks > 0) {
                hudVisibleTicks--
            }
        }

        // HUD 渲染提示当前开关状态（顶部居中白字）
        HudRenderCallback.EVENT.register { guiGraphics, _ ->
            if (hudVisibleTicks > 0) {
                val mc = Minecraft.getInstance()
                val text = if (isPlantingEnabled) "§a自动种花: 开启" else "§c自动种花: 关闭"
                val width = mc.window.guiScaledWidth
                val x = width / 2 - mc.font.width(text) / 2
                guiGraphics.drawString(mc.font, text, x, 5, 0xFFFFFF, true)
            }
        }
    }
}