import 'package:flutter/material.dart';
import 'package:rang_shop/core/localization/app_localizations.dart';

/// Показывает SnackBar "В разработке" для кнопок, у которых пока нет экрана.
void showInDevelopmentSnackBar(BuildContext context) {
  final t = AppLocalizations.of(context);
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(t.translate('inDevelopment'))));
}
