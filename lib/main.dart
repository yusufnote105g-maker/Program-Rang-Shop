import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/data/repositories/mock_auth_repository.dart';
import 'features/auth/presentation/screens/auth_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // ВСТАВЬ СЮДА СВОЙ КЛЮЧ ИЗ SUPABASE
  const String supabaseAnonKey = 'sb_publishable_LmQxA96C5NsSJf2YV-CoFQ_GyHhhfw0';
  
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
            // ЗАПУСКАЕМ ЭКРАН АВТОРИЗАЦИИ
            home: AuthScreen(
              repository: MockAuthRepository(),
              onAuthSuccess: (user) {
                print('Вход выполнен!');
              },
            ),
          );
        },
      ),
    );
  }
}
