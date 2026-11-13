import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:rang_shop/core/widgets/brand_logo.dart';


class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});


  @override
  State<SplashScreen> createState() => _SplashScreenState();
}


class _SplashScreenState extends State<SplashScreen> {
  @override
  void initState() {
    super.initState();
    Future.delayed(const Duration(seconds: 2), () {
      if (mounted) context.go('/main');
    });
  }


  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isTajik = Localizations.localeOf(context).languageCode == 'tg';
    final tagline = isTajik ? 'Услуб. Сифат. Барои шумо.' : 'Стиль. Качество. Для вас.';


    return Scaffold(
      body: Stack(
        children: [
          Positioned.fill(
            child: Image.asset(
              'assets/images/splash_bg.jpg',
              fit: BoxFit.cover,
              errorBuilder: (context, error, stackTrace) => const DecoratedBox(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [Color(0xFF0B0612), Color(0xFF14082B), Color(0xFF3A1470)],
                    stops: [0.0, 0.6, 1.0],
                  ),
                ),
              ),
            ),
          ),
          SafeArea(
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const BrandLogo(height: 150),
                  const SizedBox(height: 16),
                  Text('Rang Shop', style: theme.textTheme.headlineMedium?.copyWith(fontFamily: 'serif', fontFamilyFallback: const ['Georgia', 'Times New Roman'], fontWeight: FontWeight.w600, letterSpacing: 1.2, color: Colors.white)),
                  const SizedBox(height: 8),
                  Text(tagline, style: theme.textTheme.bodyLarge?.copyWith(color: const Color(0xFFD9C8F0))),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
