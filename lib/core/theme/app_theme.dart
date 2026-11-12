import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class AppColors {
  const AppColors._();
  static const Color brandPurple = Color(0xFF7C3AED);
  static const Color darkBackground = Color(0xFF121212);
  static const Color darkCard = Color(0xFF1E1E1E);
  static const Color lightBackground = Color(0xFFFFFFFF);
  static const Color lightCard = Color(0xFFF5F5F5);
}

class ThemeCubit extends Cubit<ThemeMode> {
  ThemeCubit() : super(ThemeMode.dark);
  void toggleTheme() {
    emit(state == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark);
  }
}

class AppTheme {
  const AppTheme._();

  static ThemeData get lightTheme => _createTheme(Brightness.light);
  static ThemeData get darkTheme => _createTheme(Brightness.dark);

  static ThemeData _createTheme(Brightness brightness) {
    final isDark = brightness == Brightness.dark;
    final background = isDark ? AppColors.darkBackground : AppColors.lightBackground;
    final card = isDark ? AppColors.darkCard : AppColors.lightCard;
    final textPrimary = isDark ? Colors.white : Colors.black;

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      scaffoldBackgroundColor: background,
      cardColor: card,
      colorScheme: ColorScheme(
        brightness: brightness,
        primary: AppColors.brandPurple,
        onPrimary: Colors.white,
        secondary: AppColors.brandPurple,
        onSecondary: Colors.white,
        error: const Color(0xFFE53935),
        onError: Colors.white,
        surface: background,
        onSurface: textPrimary,
      ),
      appBarTheme: AppBarTheme(backgroundColor: background, foregroundColor: textPrimary),
    );
  }
}
