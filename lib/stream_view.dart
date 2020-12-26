import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef void StreamViewCreatedCallback(StreamViewController controller);

class StreamView extends StatefulWidget {
  const StreamView({
    Key key,
    this.onStreamViewCreated,
    this.streamViewController,
  }) : super(key: key);

  final StreamViewCreatedCallback onStreamViewCreated;
  final StreamViewController streamViewController;

  @override
  State<StatefulWidget> createState() => _StreamViewState();
}

class _StreamViewState extends State<StreamView> {
  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(
        viewType: 'StreamView',
        onPlatformViewCreated: _onPlatformViewCreated,
      );
    }
    return Text(
        '$defaultTargetPlatform is not yet supported by the text_view plugin');
  }

  void _onPlatformViewCreated(int id) {
    if (widget.onStreamViewCreated == null) {
      return;
    }
    widget.onStreamViewCreated(new StreamViewController._(id));
  }
}

class StreamViewController {
  StreamViewController._(int id)
      : _channel = new MethodChannel('StreamView/$id');

  final MethodChannel _channel;

  Future<void> startSteaming() async {
    return _channel.invokeMethod('startStreaming');
  }

  Future<void> stopStreaming() async {
    return _channel.invokeMethod('stopStreaming');
  }
}