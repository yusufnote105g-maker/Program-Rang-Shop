/// Форматирует цену из дирамов (целое число, 100 дирамов = 1 сомони)
/// в строку вида "129 TJS" или "5 000 TJS" (как на макетах).
class PriceFormatter {
  PriceFormatter._();

  /// Если нужно вернуть "с." — поменяйте только эту константу.
  static const String currency = 'TJS';

  static String formatFromDirams(int dirams) {
    final negative = dirams < 0;
    final abs = dirams.abs();
    final whole = abs ~/ 100;
    final fraction = abs % 100;

    final fractionPart =
        fraction == 0 ? '' : ',${fraction.toString().padLeft(2, '0')}';

    return '${negative ? '-' : ''}${_group(whole)}$fractionPart $currency';
  }

  static String _group(int value) {
    final digits = value.toString();
    final buffer = StringBuffer();
    for (var i = 0; i < digits.length; i++) {
      final remaining = digits.length - i;
      buffer.write(digits[i]);
      if (remaining > 1 && remaining % 3 == 1) {
        buffer.write('\u00A0');
      }
    }
    return buffer.toString();
  }
}
