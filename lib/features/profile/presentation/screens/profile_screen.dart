import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:go_router/go_router.dart';
import 'package:rang_shop/core/localization/app_localizations.dart';
import 'package:rang_shop/core/localization/locale_cubit.dart';
import 'package:rang_shop/core/theme/app_theme.dart';
import 'package:rang_shop/core/utils/ui_helpers.dart';

/// Вкладка "Профиль". БЕЗ Scaffold (Scaffold живёт в MainScreen).
class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;

    return SafeArea(
      bottom: false,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
        children: [
          // Гость
          Center(
            child: Column(
              children: [
                CircleAvatar(
                  radius: 40,
                  backgroundColor: scheme.primaryContainer,
                  child: Icon(
                    Icons.person_outline,
                    size: 44,
                    color: scheme.onPrimaryContainer,
                  ),
                ),
                const SizedBox(height: 12),
                Text(
                  t.translate('guest'),
                  style: theme.textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  t.translate('welcome'),
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: scheme.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Баннер "Войти" -> /auth
          Material(
            color: Colors.transparent,
            borderRadius: BorderRadius.circular(24),
            clipBehavior: Clip.antiAlias,
            child: Ink(
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(24),
                gradient: const LinearGradient(
                  colors: [AppColors.accent, Color(0xFF5B1FA8)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
              child: InkWell(
                onTap: () => context.go('/auth'),
                child: Padding(
                  padding: const EdgeInsets.all(20),
                  child: Row(
                    children: [
                      const Icon(Icons.account_circle_outlined,
                          color: Colors.white, size: 40),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Text(
                          t.translate('loginToAccount'),
                          style: theme.textTheme.titleMedium?.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 10,
                        ),
                        decoration: const ShapeDecoration(
                          color: Colors.white,
                          shape: StadiumBorder(),
                        ),
                        child: Text(
                          t.translate('login'),
                          style: const TextStyle(
                            color: AppColors.accent,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 12),

          // Бонусы
          _Group(
            children: [
              ListTile(
                leading: const Icon(Icons.card_giftcard,
                    color: AppColors.accentLight),
                title: Text(
                  t.translate('bonuses'),
                  style: const TextStyle(fontWeight: FontWeight.w600),
                ),
                subtitle: Text(t.translate('points')),
                onTap: () => showInDevelopmentSnackBar(context),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Меню
          _Group(
            children: [
              _MenuTile(
                icon: Icons.inventory_2_outlined,
                title: t.translate('myOrders'),
              ),
              _MenuTile(
                icon: Icons.settings_outlined,
                title: t.translate('settings'),
              ),
              _MenuTile(
                icon: Icons.local_shipping_outlined,
                title: t.translate('deliveryTerms'),
              ),
              _MenuTile(
                icon: Icons.assignment_return_outlined,
                title: t.translate('returns'),
              ),
              _MenuTile(
                icon: Icons.headset_mic_outlined,
                title: t.translate('support'),
              ),
              _MenuTile(
                icon: Icons.info_outline,
                title: t.translate('aboutApp'),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Тема и язык
          _Group(
            children: [
              SwitchListTile(
                secondary: const Icon(Icons.dark_mode_outlined),
                title: Text(t.translate('darkTheme')),
                value: isDark,
                onChanged: (value) {
                  context.read<ThemeCubit>().setThemeMode(
                        value ? ThemeMode.dark : ThemeMode.light,
                      );
                },
              ),
              BlocBuilder<LocaleCubit, Locale>(
                builder: (context, locale) {
                  return ListTile(
                    leading: const Icon(Icons.language),
                    title: Text(t.translate('language')),
                    trailing: DropdownButton<String>(
                      value: locale.languageCode,
                      underline: const SizedBox.shrink(),
                      dropdownColor: scheme.surfaceContainerHighest,
                      items: const [
                        DropdownMenuItem<String>(
                          value: 'ru',
                          child: Text('Русский'),
                        ),
                        DropdownMenuItem<String>(
                          value: 'tg',
                          child: Text('Тоҷикӣ'),
                        ),
                      ],
                      onChanged: (code) {
                        if (code != null) {
                          context.read<LocaleCubit>().setLocale(Locale(code));
                        }
                      },
                    ),
                  );
                },
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Group extends StatelessWidget {
  const _Group({required this.children});

  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Theme.of(context).colorScheme.surface,
      borderRadius: BorderRadius.circular(20),
      clipBehavior: Clip.antiAlias,
      child: Column(children: children),
    );
  }
}

class _MenuTile extends StatelessWidget {
  const _MenuTile({required this.icon, required this.title});

  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      leading: Icon(icon),
      title: Text(title),
      trailing: const Icon(Icons.chevron_right),
      onTap: () => showInDevelopmentSnackBar(context),
    );
  }
}
