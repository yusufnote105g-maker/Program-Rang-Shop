import 'package:intl/intl.dart';

class PriceFormatter {
  const PriceFormatter._();
  static final NumberFormat _wholeFormatter = NumberFormat('#,##0', 'ru');
  static final NumberFormat _decimalFormatter = NumberFormat('#,##0.00', 'ru');

  static String formatFromDirams(int dirams) {
    final int wholeSomoni = dirams ~/ 100;
    final int remainderDirams = dirams.remainder(100).abs();
    if (remainderDirams == 0) {
      return '${_wholeFormatter.format(wholeSomoni)}\u00A0с.';
    }
    final double exactAmount = dirams / 100.0;
    return '${_decimalFormatter.format(exactAmount)}\u00A0с.';
  }

  static String formatSomoni(num somoni) {
    final int dirams = (somoni * 100).round();
    return formatFromDirams(dirams);
  }
}
