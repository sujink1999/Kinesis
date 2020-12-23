import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
        // This makes the visual density adapt to the platform that you run
        // the app on. For desktop platforms, the controls will be smaller and
        // closer together (more dense) than on mobile platforms.
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: MyPage(),
    );
  }
}

class MyPage extends StatelessWidget {
  MethodChannel platform;

  Future<void> _getBatteryLevel(BuildContext context) async {
    String batteryLevel;
    try {
      final int result = await platform.invokeMethod('getBatteryLevel');
      batteryLevel = 'Battery level at $result % .';
      print(batteryLevel);
    } on PlatformException catch (e) {
      batteryLevel = "Failed to get battery level: '${e.message}'.";
    }
  }

  Future<void> _onPlatformViewCreated(id) async{
    platform = MethodChannel("StreamView/$id");
    final result = await platform.invokeMethod('startStreaming');
  }

  @override
  Widget build(BuildContext context) {
    final mediaQuery = MediaQuery.of(context);
    return SafeArea(
      child: Scaffold(
        body: Container(
          height: mediaQuery.size.height - mediaQuery.padding.top - mediaQuery.padding.bottom,
          width: mediaQuery.size.width,
          child: Center(
            child : Container(
              padding: EdgeInsets.all(10),
              child: AndroidView(
                viewType: 'StreamView',
                onPlatformViewCreated: (id){
                  _onPlatformViewCreated(id);
                },
              ),
            ),
          ),
        )
      ),
    );
  }
}
