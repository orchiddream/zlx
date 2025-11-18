package com.example

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object Zlx : ModInitializer {
    private val logger = LoggerFactory.getLogger("zlx")

	override fun onInitialize() {
		// 这个模组主要是客户端功能，服务端不需要特殊初始化
		logger.info("自动种植机模组已加载！")
	}
}