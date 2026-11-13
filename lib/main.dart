import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rang_shop/core/localization/fallback_localizations.dart';
import 'package:rang_shop/core/localization/locale_cubit.dart';
import 'package:rang_shop/core/network/supabase_service.dart';
import 'package:rang_shop/core/router/app_router.dart';
import 'package:rang_shop/core/theme/app_theme.dart';


Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();


  try {
    await SupabaseService.initialize();
  } catch (e, stackTrace) {
    debugPrint('Supabase initialization failed: $e');
    debugPrintStack(stackTrace: stackTrace);
  }


  runApp(const RangShopApp());
}


class RangShopApp extends StatelessWidget {
  const RangShopApp({super.key});


  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider<ThemeCubit>(
          create: (_) => ThemeCubit(),
        ),
        BlocProvider<LocaleCubit>(
          create: (_) => LocaleCubit(),
        ),
      ],
      child: BlocBuilder<ThemeCubit, ThemeMode>(
        builder: (context, themeMode) {
          return BlocBuilder<LocaleCubit, Locale>(
            builder: (context, locale) {
              return MaterialApp.router(
                title: 'Rang Shop',
                debugShowCheckedModeBanner: false,
                theme: AppTheme.light,
                darkTheme: AppTheme.dark,
                themeMode: themeMode,
                locale: locale,
                supportedLocales: const [
                  Locale('ru'),
                  Locale('tg'),
                ],
                localizationsDelegates: appLocalizationsDelegates,
                routerConfig: AppRouter.router,
              );
            },
          );
        },
      ),
    );
  }
}
