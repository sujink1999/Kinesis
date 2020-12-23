package com.example.kinesis2

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.PluginRegistry

object StreamViewPlugin : FlutterPlugin{

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val viewFactory = StreamViewFactory(binding.binaryMessenger)
        binding
                .platformViewRegistry
                .registerViewFactory("StreamView", viewFactory)

    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}
}