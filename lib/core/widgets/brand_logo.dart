import 'package:flutter/material.dart';

/// Логотип Rang Shop. Если файла assets/images/logo.png нет —
/// показывается запасной вариант: Icon(Icons.shopping_bag) + Text('Rang Shop').
class BrandLogo extends StatelessWidget {
  const BrandLogo({super.key, this.height = 160});

  final double height;

  static const String assetPath = 'assets/images/logo.png';

  @override
  Widget build(BuildContext context) {
    return Image.asset(
      assetPath,
      height: height,
      fit: BoxFit.contain,
      errorBuilder: (context, error, stackTrace) =>
          _LogoFallback(height: height),
    );
  }
}

class _LogoFallback extends StatelessWidget {
  const _LogoFallback({required this.height});

  final double height;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return SizedBox(
      height: height,
      child: FittedBox(
        fit: BoxFit.scaleDown,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              Icons.shopping_bag,
              size: height * 0.55,
              color: theme.colorScheme.primary,
            ),
            const SizedBox(height: 8),
            Text(
              'Rang Shop',
              style: theme.textTheme.headlineMedium?.copyWith(
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
