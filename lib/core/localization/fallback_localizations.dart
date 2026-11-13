import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:rang_shop/core/localization/app_localizations.dart';

/// Во Flutter нет встроенных Material/Cupertino/Widgets-локализаций
/// для таджикского ('tg'). Без этих делегатов при выборе 'tg'
/// приложение падает с "No MaterialLocalizations found".
/// Для системных виджетов подставляется русский язык.

Locale _resolve(LocalizationsDelegate<dynamic> delegate, Locale locale) =>
    delegate.isSupported(locale) ? locale : const Locale('ru');

class FallbackMaterialLocalizationsDelegate
    extends LocalizationsDelegate<MaterialLocalizations> {
  const FallbackMaterialLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => true;

  @override
  Future<MaterialLocalizations> load(Locale locale) =>
      GlobalMaterialLocalizations.delegate
          .load(_resolve(GlobalMaterialLocalizations.delegate, locale));

  @override
  bool shouldReload(
          covariant LocalizationsDelegate<MaterialLocalizations> old) =>
      false;
}

class FallbackWidgetsLocalizationsDelegate
    extends LocalizationsDelegate<WidgetsLocalizations> {
  const FallbackWidgetsLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => true;

  @override
  Future<WidgetsLocalizations> load(Locale locale) =>
      GlobalWidgetsLocalizations.delegate
          .load(_resolve(GlobalWidgetsLocalizations.delegate, locale));

  @override
  bool shouldReload(
          covariant LocalizationsDelegate<WidgetsLocalizations> old) =>
      false;
}

class FallbackCupertinoLocalizationsDelegate
    extends LocalizationsDelegate<CupertinoLocalizations> {
  const FallbackCupertinoLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) => true;

  @override
  Future<CupertinoLocalizations> load(Locale locale) =>
      GlobalCupertinoLocalizations.delegate
          .load(_resolve(GlobalCupertinoLocalizations.delegate, locale));

  @override
  bool shouldReload(
          covariant LocalizationsDelegate<CupertinoLocalizations> old) =>
      false;
}

/// Список делегатов для MaterialApp.router(localizationsDelegates: ...)
final List<LocalizationsDelegate<dynamic>> appLocalizationsDelegates = [
  AppLocalizations.delegate,
  const FallbackMaterialLocalizationsDelegate(),
  const FallbackWidgetsLocalizationsDelegate(),
  const FallbackCupertinoLocalizationsDelegate(),
];
