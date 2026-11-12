Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  
  // ВСТАВЬ СЮДА СВОЙ КЛЮЧ (начинается на sb_publish...)
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
            // ВОТ ГЛАВНОЕ ИЗМЕНЕНИЕ: ЗАПУСКАЕМ ЭКРАН ВХОДА
            home: AuthScreen(
              repository: MockAuthRepository(),
              onAuthSuccess: (user) {
                print('Вход выполнен! Роль: ${user.role}');
              },
            ),
          );
        },
      ),
    );
  }
}
