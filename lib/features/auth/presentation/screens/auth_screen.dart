import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:rang_shop/core/localization/app_localizations.dart';
import 'package:rang_shop/core/widgets/brand_background.dart';
import 'package:rang_shop/core/widgets/brand_logo.dart';
import 'package:rang_shop/core/widgets/tajik_flag.dart';

/// Экран входа: только номер телефона (+992) с флагом и кнопка "Отправить код".
/// Логика AuthCubit / AuthFormState здесь НЕ используется и не изменяется.
class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final TextEditingController _phoneController = TextEditingController();
  bool _showError = false;

  @override
  void dispose() {
    _phoneController.dispose();
    super.dispose();
  }

  void _submit() {
    final digits = _phoneController.text.replaceAll(RegExp(r'\D'), '');
    if (digits.length != 9) {
      setState(() => _showError = true);
      return;
    }
    context.push('/otp', extra: '+992 ${_phoneController.text}');
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;

    return Scaffold(
      body: BrandBackground(
        child: SafeArea(
          child: Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Center(child: BrandLogo(height: 150)),
                  const SizedBox(height: 32),
                  Text(
                    t.translate('enterPhone'),
                    textAlign: TextAlign.center,
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    height: 56,
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    decoration: BoxDecoration(
                      color: scheme.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: _showError ? scheme.error : scheme.outline,
                      ),
                    ),
                    child: Row(
                      children: [
                        const TajikFlag(),
                        const SizedBox(width: 10),
                        Text(
                          '+992',
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: TextField(
                            controller: _phoneController,
                            keyboardType: TextInputType.phone,
                            inputFormatters: [_TjPhoneFormatter()],
                            style: theme.textTheme.titleMedium,
                            onChanged: (_) {
                              if (_showError) setState(() => _showError = false);
                            },
                            onSubmitted: (_) => _submit(),
                            decoration: InputDecoration(
                              border: InputBorder.none,
                              hintText: '901 23 45 67',
                              hintStyle: theme.textTheme.titleMedium?.copyWith(
                                color: scheme.onSurfaceVariant,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (_showError) ...[
                    const SizedBox(height: 8),
                    Padding(
                      padding: const EdgeInsets.only(left: 8),
                      child: Text(
                        t.translate('invalidPhone'),
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: scheme.error,
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 20),
                  FilledButton(
                    onPressed: _submit,
                    child: Text(t.translate('sendCode')),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    t.translate('termsNotice'),
                    textAlign: TextAlign.center,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: scheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Оставляет только цифры (максимум 9) и форматирует как "901 23 45 67".
class _TjPhoneFormatter extends TextInputFormatter {
  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    final digits = newValue.text.replaceAll(RegExp(r'\D'), '');
    final limited = digits.length > 9 ? digits.substring(0, 9) : digits;

    final buffer = StringBuffer();
    for (var i = 0; i < limited.length; i++) {
      if (i == 3 || i == 5 || i == 7) buffer.write(' ');
      buffer.write(limited[i]);
    }

    final text = buffer.toString();
    return TextEditingValue(
      text: text,
      selection: TextSelection.collapsed(offset: text.length),
    );
  }
}
