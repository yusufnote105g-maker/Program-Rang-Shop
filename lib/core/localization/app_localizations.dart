import 'package:flutter/widgets.dart';

class AppLocalizations {
  AppLocalizations(this.locale);

  final Locale locale;

  static const List<Locale> supportedLocales = [Locale('ru'), Locale('tg')];

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  static AppLocalizations of(BuildContext context) =>
      Localizations.of<AppLocalizations>(context, AppLocalizations)!;

  static const Map<String, Map<String, String>> _localizedValues = {
    'ru': {
      // Навигация
      'home': 'Главная',
      'catalog': 'Каталог',
      'cart': 'Корзина',
      'favorites': 'Избранное',
      'profile': 'Профиль',
      // Общие
      'login': 'Войти',
      'welcome': 'Добро пожаловать в Rang Shop',
      'inDevelopment': 'В разработке',
      'sendCode': 'Отправить код',
      'confirm': 'Подтвердить',
      'chatSoon': 'Чат-бот скоро',
      // Сплэш / вход / код
      'tagline': 'Стиль. Качество. Для вас.',
      'enterPhone': 'Введите номер телефона',
      'termsNotice': 'Продолжая, вы соглашаетесь с Условиями использования',
      'invalidPhone': 'Введите 9 цифр номера',
      'otpTitle': 'Подтверждение',
      'otpSentTo': 'Код отправлен на номер',
      'resendCode': 'Отправить код повторно',
      // Главная
      'searchHint': 'Поиск товаров...',
      'newCollection': 'Новая коллекция 2025',
      'goTo': 'Перейти',
      'clothes': 'Одежда',
      'shoes': 'Обувь',
      'accessories': 'Аксессуары',
      'beauty': 'Красота',
      'popularProducts': 'Популярные товары',
      'seeAll': 'Все',
      // Профиль
      'guest': 'Гость',
      'loginToAccount': 'Войти в аккаунт',
      'bonuses': 'Бонусы Rang Shop',
      'points': '0 баллов',
      'myOrders': 'Мои заказы',
      'settings': 'Настройки',
      'deliveryTerms': 'Условия доставки',
      'returns': 'Возврат товара',
      'support': 'Поддержка',
      'aboutApp': 'О приложении',
      'darkTheme': 'Тёмная тема',
      'language': 'Язык',
    },
    'tg': {
      'home': 'Асосӣ',
      'catalog': 'Феҳрист',
      'cart': 'Сабад',
      'favorites': 'Дӯстдошта',
      'profile': 'Профил',
      'login': 'Ворид шудан',
      'welcome': 'Хуш омадед ба Rang Shop',
      'inDevelopment': 'Дар ҳоли таҳия',
      'sendCode': 'Фиристодани рамз',
      'confirm': 'Тасдиқ кардан',
      'chatSoon': 'Чат-бот ба зудӣ',
      'tagline': 'Услуб. Сифат. Барои шумо.',
      'enterPhone': 'Рақами телефонро ворид кунед',
      'termsNotice': 'Идома дода, шумо бо Шартҳои истифода розӣ мешавед',
      'invalidPhone': '9 рақами телефонро ворид кунед',
      'otpTitle': 'Тасдиқ',
      'otpSentTo': 'Рамз ба рақам фиристода шуд',
      'resendCode': 'Рамзро дубора фиристодан',
      'searchHint': 'Ҷустуҷӯи маҳсулот...',
      'newCollection': 'Коллексияи нав 2025',
      'goTo': 'Гузаштан',
      'clothes': 'Либос',
      'shoes': 'Пойафзол',
      'accessories': 'Аксессуарҳо',
      'beauty': 'Зебоӣ',
      'popularProducts': 'Маҳсулоти маъмул',
      'seeAll': 'Ҳама',
      'guest': 'Меҳмон',
      'loginToAccount': 'Ворид шудан ба аккаунт',
      'bonuses': 'Бонусҳои Rang Shop',
      'points': '0 хол',
      'myOrders': 'Фармоишҳои ман',
      'settings': 'Танзимот',
      'deliveryTerms': 'Шартҳои расонидан',
      'returns': 'Баргардонидани маҳсулот',
      'support': 'Дастгирӣ',
      'aboutApp': 'Дар бораи барнома',
      'darkTheme': 'Мавзӯи торик',
      'language': 'Забон',
    },
  };

  String translate(String key) {
    return _localizedValues[locale.languageCode]?[key] ??
        _localizedValues['ru']![key] ??
        key;
  }
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) =>
      ['ru', 'tg'].contains(locale.languageCode);

  @override
  Future<AppLocalizations> load(Locale locale) =>
      SynchronousFuture<AppLocalizations>(AppLocalizations(locale));

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
