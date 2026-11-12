import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'core/theme/app_theme.dart';
import 'core/utils/price_formatter.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // ВСТАВЬ СЮДА СВОЙ КЛЮЧ (тот, что начинается на sb_publish...)
  const String supabaseAnonKey = 'ВСТАВЬ_СЮДА_КЛЮЧ_sb_publish...';
  
  await Supabase.initialize(
    url: 'https://kuyyrmxjplglqdfffyre.supabase.co',
    anonKey: supabaseAnonKey,
  );
  
  runApp(const RangShopApp());
}

class RangShopApp extends StatelessWidget {
  const RangShopApp({super.key});

  @override
  Widget build(BuildContext context) {
    return BlocProvider<ThemeCubit>(
      create: (_) => ThemeCubit(),
      child: BlocBuilder<ThemeCubit, ThemeMode>(
        builder: (context, themeMode) {
          return MaterialApp(
            title: 'Rang Shop',
            debugShowCheckedModeBanner: false,
            theme: AppTheme.lightTheme,
            darkTheme: AppTheme.darkTheme,
            themeMode: themeMode,
            home: const FoundationCheckScreen(),
          );
        },
      ),
    );
  }
}

class FoundationCheckScreen extends StatelessWidget {
  const FoundationCheckScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Rang Shop — Ready'),
        actions: [
          IconButton(
            onPressed: () => context.read<ThemeCubit>().toggleTheme(),
            icon: Icon(
              theme.brightness == Brightness.dark
                  ? Icons.light_mode_outlined
                  : Icons.dark_mode_outlined,
            ),
          ),
        ],
      ),
      body: Center(
        child: Container(
          padding: const EdgeInsets.all(24),
          margin: const EdgeInsets.symmetric(horizontal: 24),
          decoration: BoxDecoration(
            color: theme.cardColor,
            borderRadius: BorderRadius.circular(20),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.verified_outlined,
                size: 56,
                color: theme.colorScheme.primary,
              ),
              const SizedBox(height: 16),
              Text(
                'Supabase подключен успешно!',
                style: theme.textTheme.titleMedium,
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              Text(
                'Цена из БД (45000 дирамов): ${PriceFormatter.formatFromDirams(45000)}',
                style: theme.textTheme.bodyMedium,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
