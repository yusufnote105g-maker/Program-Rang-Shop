import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

/// Палитра Rang Shop (по макетам: тёмно-фиолетовая тема).
class AppColors {
  AppColors._();

  static const Color accent = Color(0xFF8A2BE2);
  static const Color accentLight = Color(0xFFB57BEE);
  static const Color star = Color(0xFFFFB800);
  static const Color discount = Color(0xFFE5173F);

  static const Color darkBackground = Color(0xFF0B0612);
  static const Color darkSurface = Color(0xFF171025);
  static const Color darkSurfaceHigh = Color(0xFF21173A);
  static const Color darkOutline = Color(0xFF3A2A5C);

  static const Color lightBackground = Color(0xFFF7F3FC);
  static const Color lightSurface = Color(0xFFFFFFFF);
  static const Color lightSurfaceHigh = Color(0xFFEFE6FA);
  static const Color lightOutline = Color(0xFFD8C9EE);
}

/// Управляет режимом темы. По макетам приложение стартует в тёмной теме.
class ThemeCubit extends Cubit<ThemeMode> {
  ThemeCubit() : super(ThemeMode.dark);

  void setThemeMode(ThemeMode mode) {
    if (mode != state) emit(mode);
  }

  void toggle() {
    emit(state == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark);
  }
}

class AppTheme {
  AppTheme._();

  static ThemeData get dark => _build(_darkScheme, AppColors.darkBackground);
  static ThemeData get light => _build(_lightScheme, AppColors.lightBackground);

  // Алиасы на случай, если старый код использует эти имена.
  static ThemeData get darkTheme => dark;
  static ThemeData get lightTheme => light;

  static const ColorScheme _darkScheme = ColorScheme(
    brightness: Brightness.dark,
    primary: AppColors.accent,
    onPrimary: Colors.white,
    primaryContainer: Color(0xFF3B1A66),
    onPrimaryContainer: Color(0xFFE9D7FF),
    secondary: AppColors.accentLight,
    onSecondary: Colors.black,
    secondaryContainer: Color(0xFF2A1450),
    onSecondaryContainer: Color(0xFFE3CCFF),
    error: Color(0xFFFF5C7A),
    onError: Colors.white,
    surface: AppColors.darkSurface,
    onSurface: Colors.white,
    surfaceContainerHighest: AppColors.darkSurfaceHigh,
    onSurfaceVariant: Color(0xFFB9AECF),
    outline: AppColors.darkOutline,
    outlineVariant: Color(0xFF2B1F46),
  );

  static const ColorScheme _lightScheme = ColorScheme(
    brightness: Brightness.light,
    primary: AppColors.accent,
    onPrimary: Colors.white,
    primaryContainer: Color(0xFFEBD9FF),
    onPrimaryContainer: Color(0xFF3B1A66),
    secondary: AppColors.accentLight,
    onSecondary: Colors.white,
    secondaryContainer: Color(0xFFEBD9FF),
    onSecondaryContainer: Color(0xFF3B1A66),
    error: Color(0xFFD62048),
    onError: Colors.white,
    surface: AppColors.lightSurface,
    onSurface: Color(0xFF1B1230),
    surfaceContainerHighest: AppColors.lightSurfaceHigh,
    onSurfaceVariant: Color(0xFF6B5F82),
    outline: AppColors.lightOutline,
    outlineVariant: Color(0xFFE6DAF5),
  );

  static ThemeData _build(ColorScheme scheme, Color background) {
    const buttonText = TextStyle(fontSize: 16, fontWeight: FontWeight.w600);

    return ThemeData(
      useMaterial3: true,
      brightness: scheme.brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor: background,
      // Все кнопки — StadiumBorder.
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: AppColors.accent,
          foregroundColor: Colors.white,
          minimumSize: const Size(64, 52),
          shape: const StadiumBorder(),
          textStyle: buttonText,
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.accent,
          foregroundColor: Colors.white,
          elevation: 0,
          minimumSize: const Size(64, 52),
          shape: const StadiumBorder(),
          textStyle: buttonText,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: scheme.onSurface,
          minimumSize: const Size(64, 52),
          shape: const StadiumBorder(),
          side: const BorderSide(color: AppColors.accent),
          textStyle: buttonText,
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.accentLight,
          shape: const StadiumBorder(),
        ),
      ),
      floatingActionButtonTheme: const FloatingActionButtonThemeData(
        backgroundColor: AppColors.accent,
        foregroundColor: Colors.white,
        shape: StadiumBorder(),
      ),
      navigationBarTheme: NavigationBarThemeData(
        height: 68,
        elevation: 0,
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        indicatorColor: scheme.secondaryContainer,
        labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
      ),
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: scheme.surface,
        surfaceTintColor: Colors.transparent,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(28)),
        ),
      ),
    );
  }
}
