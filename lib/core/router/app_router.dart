import 'package:go_router/go_router.dart';
import 'package:rang_shop/features/auth/presentation/screens/auth_screen.dart';
import 'package:rang_shop/features/auth/presentation/screens/otp_screen.dart';
import 'package:rang_shop/features/main/presentation/main_screen.dart';
import 'package:rang_shop/features/splash/presentation/splash_screen.dart';

class AppRouter {
  AppRouter._();

  static final GoRouter router = GoRouter(
    initialLocation: '/',
    routes: [
      GoRoute(
        path: '/',
        builder: (context, state) => const SplashScreen(),
      ),
      GoRoute(
        path: '/auth',
        builder: (context, state) => const AuthScreen(),
      ),
      GoRoute(
        path: '/otp',
        builder: (context, state) {
          final extra = state.extra;
          return OtpScreen(phone: extra is String ? extra : '+992');
        },
      ),
      GoRoute(
        path: '/main',
        builder: (context, state) => const MainScreen(),
      ),
    ],
  );
}
