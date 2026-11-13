import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:rang_shop/core/widgets/brand_logo.dart';


class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});
  @override
  State<AuthScreen> createState() => _AuthScreenState();
}


class _AuthScreenState extends State<AuthScreen> {
  static const int _phoneLength = 9;
  final TextEditingController _controller = TextEditingController();


  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }


  int _digitsCount(String text) => text.replaceAll(RegExp(r'\D'), '').length;


  void _submit() {
    if (_digitsCount(_controller.text) != _phoneLength) return;
    context.push('/otp', extra: '+992 ${_controller.text}');
  }


  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;
    final isTajik = Localizations.localeOf(context).languageCode == 'tg';


    final enterPhone = isTajik ? 'Рақами телефонро ворид кунед' : 'Введите номер телефона';
    final invalidPhone = isTajik ? '9 рақами телефонро ворид кунед' : 'Введите 9 цифр номера';
    final sendCode = isTajik ? 'Фиристодани рамз' : 'Отправить код';
    final terms = isTajik ? 'Идома дода, шумо бо Шартҳои истифода розӣ мешавед' : 'Продолжая, вы соглашаетесь с Условиями использования';


    return Scaffold(
      body: DecoratedBox(
        decoration: BoxDecoration(
          color: isDark ? null : theme.scaffoldBackgroundColor,
          gradient: isDark ? const LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0xFF0B0612), Color(0xFF14082B), Color(0xFF3A1470)], stops: [0.0, 0.6, 1.0]) : null,
        ),
        child: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 24),
                child: ConstrainedBox(
                  constraints: BoxConstraints(minHeight: constraints.maxHeight),
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const SizedBox(height: 24),
                          const Center(child: BrandLogo(height: 80)),
                          const SizedBox(height: 32),
                          Text(enterPhone, textAlign: TextAlign.center, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600)),
                          const SizedBox(height: 16),
                          ValueListenableBuilder<TextEditingValue>(
                            valueListenable: _controller,
                            builder: (context, value, _) {
                              final digits = _digitsCount(value.text);
                              final isValid = digits == _phoneLength;
                              final showError = digits > 0 && !isValid;


                              return Column(
                                crossAxisAlignment: CrossAxisAlignment.stretch,
                                children: [
                                  Container(
                                    height: 56,
                                    padding: const EdgeInsets.symmetric(horizontal: 16),
                                    decoration: BoxDecoration(
                                      color: scheme.surfaceContainerHighest,
                                      borderRadius: BorderRadius.circular(16),
                                      border: Border.all(color: showError ? scheme.error : scheme.outline),
                                    ),
                                    child: Row(
                                      children: [
                                        ClipRRect(
                                          borderRadius: BorderRadius.circular(3),
                                          child: Image.asset('assets/images/tajik_flag.jpg', width: 30, height: 20, fit: BoxFit.cover, errorBuilder: (context, error, stackTrace) => const Icon(Icons.flag_outlined, size: 22)),
                                        ),
                                        const SizedBox(width: 10),
                                        Text('+992 ', style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600)),
                                        Expanded(
                                          child: TextField(
                                            controller: _controller,
                                            keyboardType: TextInputType.phone,
                                            inputFormatters: [_TjPhoneFormatter()],
                                            style: theme.textTheme.titleMedium,
                                            onSubmitted: (_) => _submit(),
                                            decoration: InputDecoration(
                                              border: InputBorder.none,
                                              hintText: '901 23 45 67',
                                              hintStyle: theme.textTheme.titleMedium?.copyWith(color: scheme.onSurfaceVariant),
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                  if (showError) ...[
                                    const SizedBox(height: 8),
                                    Padding(padding: const EdgeInsets.only(left: 8), child: Text(invalidPhone, style: theme.textTheme.bodySmall?.copyWith(color: scheme.error))),
                                  ],
                                  const SizedBox(height: 20),
                                  FilledButton(onPressed: isValid ? _submit : null, child: Text(sendCode)),
                                ],
                              );
                            },
                          ),
                        ],
                      ),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          const SizedBox(height: 24),
                          Center(child: Image.asset('assets/images/auth_bag.jpg', height: 150, fit: BoxFit.contain, errorBuilder: (context, error, stackTrace) => const SizedBox(height: 150))),
                          const SizedBox(height: 16),
                          Text(terms, textAlign: TextAlign.center, style: theme.textTheme.bodySmall?.copyWith(color: scheme.onSurfaceVariant)),
                          const SizedBox(height: 24),
                        ],
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}


class _TjPhoneFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
    final digits = newValue.text.replaceAll(RegExp(r'\D'), '');
    final limited = digits.length > 9 ? digits.substring(0, 9) : digits;
    final buffer = StringBuffer();
    for (var i = 0; i < limited.length; i++) {
      if (i == 3 || i == 5 || i == 7) buffer.write(' ');
      buffer.write(limited[i]);
    }
    final text = buffer.toString();
    return TextEditingValue(text: text, selection: TextSelection.collapsed(offset: text.length));
  }
}
