import 'package:flutter/material.dart';

/// Фон для сплэша / входа / кода: в тёмной теме — фиолетовый градиент
/// как на макетах, в светлой — обычный фон.
class BrandBackground extends StatelessWidget {
  const BrandBackground({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    return SizedBox.expand(
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: isDark ? null : theme.scaffoldBackgroundColor,
          gradient: isDark
              ? const LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Color(0xFF0B0612),
                    Color(0xFF14082B),
                    Color(0xFF3A1470),
                  ],
                  stops: [0.0, 0.6, 1.0],
                )
              : null,
        ),
        child: child,
      ),
    );
  }
}
