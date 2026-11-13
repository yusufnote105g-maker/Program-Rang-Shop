import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rang_shop/core/state/app_cubits.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:rang_shop/core/localization/app_localizations.dart';
import 'package:rang_shop/core/widgets/brand_background.dart';

class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, this.phone = '+992'});

  /// Номер, на который "отправлен" код (передаётся через GoRouter extra).
  final String phone;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  static const int _length = 4;
  static const int _resendSeconds = 59;

  final List<TextEditingController> _controllers =
      List.generate(_length, (_) => TextEditingController());
  final List<FocusNode> _focusNodes =
      List.generate(_length, (_) => FocusNode());

  Timer? _timer;
  int _secondsLeft = _resendSeconds;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  @override
  void dispose() {
    _timer?.cancel();
    for (final c in _controllers) {
      c.dispose();
    }
    for (final f in _focusNodes) {
      f.dispose();
    }
    super.dispose();
  }

  void _startTimer() {
    _timer?.cancel();
    _secondsLeft = _resendSeconds;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }
      if (_secondsLeft <= 1) {
        timer.cancel();
        setState(() => _secondsLeft = 0);
        return;
      }
      setState(() => _secondsLeft--);
    });
  }

  void _resend() {
    setState(() => _secondsLeft = _resendSeconds);
    _startTimer();
  }

  void _onChanged(int index, String value) {
    if (value.isNotEmpty && index < _length - 1) {
      _focusNodes[index + 1].requestFocus();
    } else if (value.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }
  }

  void _goBack() {
    if (context.canPop()) {
      context.pop();
    } else {
      context.go('/auth');
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final scheme = theme.colorScheme;
    final timerText = '00:${_secondsLeft.toString().padLeft(2, '0')}';

    return Scaffold(
      body: BrandBackground(
        child: SafeArea(
          child: Column(
            children: [
              Align(
                alignment: Alignment.centerLeft,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back_ios_new),
                  onPressed: _goBack,
                ),
              ),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(horizontal: 24),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const SizedBox(height: 24),
                      Text(
                        t.translate('otpTitle'),
                        textAlign: TextAlign.center,
                        style: theme.textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 16),
                      Text(
                        t.translate('otpSentTo'),
                        textAlign: TextAlign.center,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          color: scheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        widget.phone,
                        textAlign: TextAlign.center,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 32),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: List.generate(_length, (index) {
                          return Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 8),
                            child: SizedBox(
                              width: 56,
                              child: TextField(
                                controller: _controllers[index],
                                focusNode: _focusNodes[index],
                                autofocus: index == 0,
                                textAlign: TextAlign.center,
                                keyboardType: TextInputType.number,
                                style: theme.textTheme.headlineSmall,
                                inputFormatters: [
                                  FilteringTextInputFormatter.digitsOnly,
                                  LengthLimitingTextInputFormatter(1),
                                ],
                                decoration: InputDecoration(
                                  filled: true,
                                  fillColor: scheme.surfaceContainerHighest,
                                  contentPadding:
                                      const EdgeInsets.symmetric(vertical: 16),
                                  enabledBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(14),
                                    borderSide:
                                        BorderSide(color: scheme.outline),
                                  ),
                                  focusedBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(14),
                                    borderSide: BorderSide(
                                      color: scheme.primary,
                                      width: 2,
                                    ),
                                  ),
                                ),
                                onChanged: (value) => _onChanged(index, value),
                              ),
                            ),
                          );
                        }),
                      ),
                      const SizedBox(height: 24),
                      if (_secondsLeft > 0)
                        Text(
                          '${t.translate('resendCode')} ($timerText)',
                          textAlign: TextAlign.center,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: scheme.onSurfaceVariant,
                          ),
                        )
                      else
                        TextButton(
                          onPressed: _resend,
                          child: Text(t.translate('resendCode')),
                        ),
                      const SizedBox(height: 32),
                      FilledButton(
                        onPressed: () {
                          context.read<AuthSessionCubit>().signIn();
                          context.go('/main');
                        },
                        child: Text(t.translate('confirm')),
                      ),
                      const SizedBox(height: 24),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
