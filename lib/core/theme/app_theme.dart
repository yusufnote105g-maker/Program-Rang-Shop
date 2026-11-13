// lib/core/theme/app_theme.dart


import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';


class AppColors {
  const AppColors._();


  static const Color brandPurple = Color(0xFF7C3AED);
  static const Color brandPurpleDisabled = Color(0x807C3AED);
  static const Color errorRed = Color(0xFFE53935);
  static const Color alifGreen = Color(0xFF00C853);


  static const Color darkBackground = Color(0xFF121212);
  static const Color darkCard = Color(0xFF1E1E1E);
  static const Color darkTextPrimary = Color(0xFFFFFFFF);
  static const Color darkTextSecondary = Color(0xFFAAAAAA);


  static const Color lightBackground = Color(0xFFFFFFFF);
  static const Color lightCard = Color(0xFFF5F5F5);
  static const Color lightTextPrimary = Color(0xFF121212);
  static const Color lightTextSecondary = Color(0xFF666666);
}


class ThemeCubit extends Cubit<ThemeMode> {
  ThemeCubit() : super(ThemeMode.dark);


  void toggleTheme() {
    emit(state == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark);
  }


  void setThemeMode(ThemeMode mode) => emit(mode);
}


class AppTheme {
  const AppTheme._();


  static ThemeData get lightTheme => _createTheme(
        brightness: Brightness.light,
        background: AppColors.lightBackground,
        card: AppColors.lightCard,
        textPrimary: AppColors.lightTextPrimary,
        textSecondary: AppColors.lightTextSecondary,
      );


  static ThemeData get darkTheme => _createTheme(
        brightness: Brightness.dark,
        background: AppColors.darkBackground,
        card: AppColors.darkCard,
        textPrimary: AppColors.darkTextPrimary,
        textSecondary: AppColors.darkTextSecondary,
      );


  static ThemeData _createTheme({
    required Brightness brightness,
    required Color background,
    required Color card,
    required Color textPrimary,
    required Color textSecondary,
  }) {
    final ColorScheme colorScheme = ColorScheme(
      brightness: brightness,
      primary: AppColors.brandPurple,
      onPrimary: Colors.white,
      secondary: AppColors.brandPurple,
      onSecondary: Colors.white,
      error: AppColors.errorRed,
      onError: Colors.white,
      surface: background,
      onSurface: textPrimary,
      surfaceContainerHighest: card,
    );


    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      primaryColor: AppColors.brandPurple,
      scaffoldBackgroundColor: background,
      cardColor: card,
      colorScheme: colorScheme,
      appBarTheme: AppBarTheme(
        backgroundColor: background,
        foregroundColor: textPrimary,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: card,
        contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
        hintStyle: TextStyle(color: textSecondary, fontSize: 15),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: BorderSide.none,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: BorderSide.none,
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: const BorderSide(color: AppColors.brandPurple, width: 1.5),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: const BorderSide(color: AppColors.errorRed, width: 1.5),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(28),
          borderSide: const BorderSide(color: AppColors.errorRed, width: 1.5),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.brandPurple,
          foregroundColor: Colors.white,
          disabledBackgroundColor: AppColors.brandPurpleDisabled,
          disabledForegroundColor: Colors.white70,
          elevation: 0,
          minimumSize: const Size(64, 56),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(28),
          ),
          textStyle: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }
}
