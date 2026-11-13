// lib/features/auth/presentation/screens/auth_screen.dart


import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';


import 'package:rang_shop/features/auth/domain/repositories/auth_repository.dart';
import 'package:rang_shop/features/auth/presentation/bloc/auth_cubit.dart';
import 'package:rang_shop/features/auth/presentation/bloc/auth_form_state.dart';


class AuthScreen extends StatelessWidget {
  const AuthScreen({
    super.key,
    required this.repository,
    this.onAuthSuccess,
    this.onForgotPassword,
    this.onRegister,
  });


  final AuthRepository repository;
  final ValueChanged<UserEntity>? onAuthSuccess;
  final VoidCallback? onForgotPassword;
  final VoidCallback? onRegister;


  @override
  Widget build(BuildContext context) {
    return BlocProvider<AuthCubit>(
      create: (_) => AuthCubit(repository: repository),
      child: _AuthView(
        onAuthSuccess: onAuthSuccess,
        onForgotPassword: onForgotPassword,
        onRegister: onRegister,
      ),
    );
  }
}


class _AuthView extends StatefulWidget {
  const _AuthView({
    required this.onAuthSuccess,
    required this.onForgotPassword,
    required this.onRegister,
  });


  final ValueChanged<UserEntity>? onAuthSuccess;
  final VoidCallback? onForgotPassword;
  final VoidCallback? onRegister;


  @override
  State<_AuthView> createState() => _AuthViewState();
}


class _AuthViewState extends State<_AuthView> {
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();


  @override
  void dispose() {
    _phoneController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }


  void _submit() {
    FocusScope.of(context).unfocus();
    context.read<AuthCubit>().signIn(
          phone: _phoneController.text,
          email: _emailController.text,
          password: _passwordController.text,
        );
  }


  void _showSnackBar(
    String message, {
    required Color background,
    required Color foreground,
  }) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message, style: TextStyle(color: foreground)),
          backgroundColor: background,
          behavior: SnackBarBehavior.floating,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
      );
  }


  @override
  Widget build(BuildContext context) {
    final ThemeData theme = Theme.of(context);
    final ColorScheme scheme = theme.colorScheme;
    final Color secondary = scheme.onSurface.withValues(alpha: 0.6);


    return BlocConsumer<AuthCubit, AuthFormState>(
      listenWhen: (previous, current) =>
          (current.status == AuthFormStatus.error &&
              current.errorId != previous.errorId) ||
          (current.status == AuthFormStatus.success &&
              previous.status != AuthFormStatus.success),
      listener: (context, state) {
        if (state.status == AuthFormStatus.error) {
          final String? message = state.errorMessage;
          if (message == null) return;
          _showSnackBar(
            message,
            background: scheme.error,
            foreground: scheme.onError,
          );
          return;
        }


        final UserEntity? user = state.user;
        final ValueChanged<UserEntity>? onSuccess = widget.onAuthSuccess;
        if (user != null && onSuccess != null) {
          onSuccess(user);
        } else {
          _showSnackBar(
            'Вход выполнен успешно!',
            background: scheme.primary,
            foreground: scheme.onPrimary,
          );
        }
      },
      builder: (context, state) {
        final AuthCubit cubit = context.read<AuthCubit>();
        final bool isLoading = state.isLoading;


        return Scaffold(
          body: SafeArea(
            child: SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 32),
              child: AutofillGroup(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const SizedBox(height: 24),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: scheme.primary.withValues(alpha: 0.15),
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            Icons.shopping_bag_rounded,
                            color: scheme.primary,
                            size: 32,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Text(
                          'Rang Shop',
                          style: theme.textTheme.headlineSmall?.copyWith(
                            fontWeight: FontWeight.bold,
                            letterSpacing: 0.5,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 48),
                    Text(
                      'С возвращением!',
                      style: theme.textTheme.headlineMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Войдите в свой аккаунт, чтобы продолжить покупки.',
                      style: theme.textTheme.bodyLarge?.copyWith(
                        color: secondary,
                      ),
                    ),
                    const SizedBox(height: 32),
                    SegmentedButton<bool>(
                      showSelectedIcon: false,
                      expandedInsets: EdgeInsets.zero,
                      segments: const [
                        ButtonSegment<bool>(
                          value: true,
                          label: Text('Телефон'),
                        ),
                        ButtonSegment<bool>(
                          value: false,
                          label: Text('Email'),
                        ),
                      ],
                      selected: {state.isPhoneLogin},
                      onSelectionChanged: isLoading
                          ? null
                          : (selection) {
                              FocusScope.of(context).unfocus();
                              cubit.setLoginMode(selection.first);
                            },
                      style: SegmentedButton.styleFrom(
                        selectedBackgroundColor: scheme.primary,
                        selectedForegroundColor: scheme.onPrimary,
                      ),
                    ),
                    const SizedBox(height: 24),
                    if (state.isPhoneLogin)
                      TextField(
                        key: const ValueKey('auth_phone_text_field'),
                        controller: _phoneController,
                        enabled: !isLoading,
                        keyboardType: TextInputType.phone,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [
                          AutofillHints.telephoneNumberNational,
                        ],
                        inputFormatters: [_TajikPhoneFormatter()],
                        style: const TextStyle(fontSize: 16, letterSpacing: 0.5),
                        decoration: InputDecoration(
                          hintText: '90 123 4567',
                          prefixIconConstraints: const BoxConstraints(
                            minWidth: 0,
                            minHeight: 0,
                          ),
                          prefixIcon: Padding(
                            padding: const EdgeInsets.only(left: 18, right: 12),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Text('🇹🇯', style: TextStyle(fontSize: 20)),
                                const SizedBox(width: 8),
                                const Text(
                                  '+992',
                                  style: TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                const SizedBox(width: 10),
                                Container(
                                  width: 1,
                                  height: 22,
                                  color: scheme.onSurface.withValues(alpha: 0.2),
                                ),
                              ],
                            ),
                          ),
                        ),
                      )
                    else
                      TextField(
                        key: const ValueKey('auth_email_text_field'),
                        controller: _emailController,
                        enabled: !isLoading,
                        keyboardType: TextInputType.emailAddress,
                        textInputAction: TextInputAction.next,
                        autofillHints: const [AutofillHints.email],
                        autocorrect: false,
                        enableSuggestions: false,
                        style: const TextStyle(fontSize: 16),
                        decoration: InputDecoration(
                          hintText: 'example@rangshop.tj',
                          prefixIcon: Icon(
                            Icons.alternate_email_rounded,
                            color: secondary,
                            size: 22,
                          ),
                        ),
                      ),
                    const SizedBox(height: 16),
                    TextField(
                      key: const ValueKey('auth_password_text_field'),
                      controller: _passwordController,
                      enabled: !isLoading,
                      obscureText: state.obscurePassword,
                      autocorrect: false,
                      enableSuggestions: false,
                      textInputAction: TextInputAction.done,
                      autofillHints: const [AutofillHints.password],
                      onSubmitted: (_) => _submit(),
                      style: const TextStyle(fontSize: 16),
                      decoration: InputDecoration(
                        hintText: 'Введите пароль',
                        prefixIcon: Icon(
                          Icons.lock_outline_rounded,
                          color: secondary,
                          size: 22,
                        ),
                        suffixIcon: IconButton(
                          tooltip: state.obscurePassword
                              ? 'Показать пароль'
                              : 'Скрыть пароль',
                          onPressed:
                              isLoading ? null : cubit.togglePasswordVisibility,
                          icon: Icon(
                            state.obscurePassword
                                ? Icons.visibility_off_outlined
                                : Icons.visibility_outlined,
                            color: secondary,
                            size: 22,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: isLoading ? null : widget.onForgotPassword,
                        child: const Text(
                          'Забыли пароль?',
                          style: TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    FilledButton(
                      onPressed: isLoading ? null : _submit,
                      style: FilledButton.styleFrom(
                        minimumSize: const Size.fromHeight(56),
                        shape: const StadiumBorder(),
                        disabledBackgroundColor:
                            scheme.primary.withValues(alpha: 0.5),
                        disabledForegroundColor: scheme.onPrimary,
                      ),
                      child: isLoading
                          ? SizedBox(
                              width: 24,
                              height: 24,
                              child: CircularProgressIndicator(
                                strokeWidth: 2.5,
                                color: scheme.onPrimary,
                              ),
                            )
                          : const Text(
                              'Войти',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                    ),
                    const SizedBox(height: 24),
                    Wrap(
                      alignment: WrapAlignment.center,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Text(
                          'Нет аккаунта?',
                          style: theme.textTheme.bodyMedium?.copyWith(
                            color: secondary,
                          ),
                        ),
                        TextButton(
                          onPressed: isLoading ? null : widget.onRegister,
                          child: const Text(
                            'Зарегистрироваться',
                            style: TextStyle(fontWeight: FontWeight.bold),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}


class _TajikPhoneFormatter extends TextInputFormatter {
  static final RegExp _nonDigits = RegExp(r'\D');


  @override
  TextEditingValue formatEditUpdate(
    TextEditingValue oldValue,
    TextEditingValue newValue,
  ) {
    String digits = newValue.text.replaceAll(_nonDigits, '');


    if (digits.length == 12 && digits.startsWith('992')) {
      digits = digits.substring(3);
    } else if (digits.length == 10 && digits.startsWith('0')) {
      digits = digits.substring(1);
    }


    if (digits.length > 9) {
      digits = digits.substring(0, 9);
    }


    if (digits == newValue.text) return newValue;


    return TextEditingValue(
      text: digits,
      selection: TextSelection.collapsed(offset: digits.length),
    );
  }
}
