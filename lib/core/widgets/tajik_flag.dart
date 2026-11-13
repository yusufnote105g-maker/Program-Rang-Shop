import 'package:flutter/material.dart';

/// Флаг Таджикистана (красный / белый / зелёный), рисуется без картинок.
class TajikFlag extends StatelessWidget {
  const TajikFlag({super.key, this.width = 30, this.height = 20});

  final double width;
  final double height;

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(3),
      child: SizedBox(
        width: width,
        height: height,
        child: const Column(
          children: [
            Expanded(flex: 2, child: ColoredBox(color: Color(0xFFCC0000))),
            Expanded(flex: 3, child: ColoredBox(color: Colors.white)),
            Expanded(flex: 2, child: ColoredBox(color: Color(0xFF006600))),
          ],
        ),
      ),
    );
  }
}
