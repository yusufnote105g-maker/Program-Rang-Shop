import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rang_shop/core/network/supabase_service.dart';
import 'package:rang_shop/core/theme/app_theme.dart';
import 'package:rang_shop/features/auth/data/repositories/mock_auth_repository.dart';
import 'package:rang_shop/features/auth/domain/repositories/auth_repository.dart';
import 'package:rang_shop/features/catalog/data/repositories/mock_product_repository.dart';
import 'package:rang_shop/features/catalog/domain/repositories/product_repository.dart';
import 'package:rang_shop/features/catalog/presentation/screens/home_screen.dart';


Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();


  // Ошибка Supabase не должна оставлять пустой экран: приложение
  // запускается в любом случае, пока работает на моках.
  try {
    await SupabaseService.initialize();
  } catch (error, stackTrace) {
    debugPrint('Supabase не инициализирован: $error\n$stackTrace');
  }


  // Репозитории создаются один раз, чтобы не терять состояние
  // при пересборке приложения (например, при смене темы).
  // MockAuthRepository работает только в debug-сборке.
  runApp(
    RangShopApp(
      authRepository: MockAuthRepository(),
      productRepository: const MockProductRepository(),
    ),
  );
}


class RangShopApp extends StatelessWidget {
  const RangShopApp({
    super.key,
    required this.authRepository,
    required this.productRepository,
  });


  final AuthRepository authRepository;
  final ProductRepository productRepository;


  @override
  Widget build(BuildContext context) {
    return BlocProvider<ThemeCubit>(
      create: (_) => ThemeCubit(),
      child: BlocBuilder<ThemeCubit, ThemeMode>(
        builder: (BuildContext context, ThemeMode themeMode) {
          return MaterialApp(
            title: 'Rang Shop',
            debugShowCheckedModeBanner: false,
            theme: AppTheme.lightTheme,
            darkTheme: AppTheme.darkTheme,
            themeMode: themeMode,
            home: HomeScreen(
              authRepository: authRepository,
              productRepository: productRepository,
            ),
          );
        },
      ),
    );
  }
}
