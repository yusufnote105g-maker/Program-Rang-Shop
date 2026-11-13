import 'dart:ui';

import 'package:flutter_bloc/flutter_bloc.dart';

class LocaleCubit extends Cubit<Locale> {
  LocaleCubit() : super(const Locale('ru'));

  void setLocale(Locale locale) {
    if (locale.languageCode == state.languageCode) return;
    emit(locale);
  }
}
