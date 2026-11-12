import 'package:flutter_bloc/flutter_bloc.dart';

import '../../domain/repositories/auth_repository.dart';
import 'auth_form_state.dart';

class AuthCubit extends Cubit<AuthFormState> {
  AuthCubit({required AuthRepository repository})
      : _repository = repository,
        super(const AuthFormState());

  final AuthRepository _repository;

  /// 9 цифр национального номера, без ведущего нуля.
  static final RegExp _phoneRegex = RegExp(r'^[1-9]\d{8}$');

  /// Что-то@что-то.домен (минимум 2 символа в домене), без пробелов.
  static final RegExp _emailRegex = RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]{2,}$');

  static const int _minPasswordLength = 6;
  static const String _countryCode = '+992';

  /// Переключение «Телефон» / «Email». Во время загрузки игнорируется.
  void setLoginMode(bool isPhone) {
    if (state.isLoading || state.isPhoneLogin == isPhone) return;
    emit(
      state.copyWith(
        isPhoneLogin: isPhone,
        status: AuthFormStatus.initial,
        clearError: true,
      ),
    );
  }

  /// Видимость пароля. Статус формы не трогает, поэтому не сбрасывает
  /// загрузку и не вызывает повторных SnackBar.
  void togglePasswordVisibility() {
    emit(state.copyWith(obscurePassword: !state.obscurePassword));
  }

  Future<void> signIn({
    required String phone,
    required String email,
    required String password,
  }) async {
    // Защита от повторной отправки во время запроса.
    if (state.isLoading) return;

    final bool isPhone = state.isPhoneLogin;
    final String cleanPhone = phone.trim();
    final String cleanEmail = email.trim().toLowerCase();
    // Пароль намеренно не обрезается: пробелы могут быть его частью.

    final String? validationError = _validate(
      isPhone: isPhone,
      phone: cleanPhone,
      email: cleanEmail,
      password: password,
    );
    if (validationError != null) {
      _emitError(validationError);
      return;
    }

    emit(
      state.copyWith(
        status: AuthFormStatus.loading,
        clearError: true,
        clearUser: true,
      ),
    );

    try {
      final UserEntity user = isPhone
          ? await _repository.signInWithPhonePassword(
              phone: '$_countryCode$cleanPhone',
              password: password,
            )
          : await _repository.signInWithEmailPassword(
              email: cleanEmail,
              password: password,
            );

      // Пользователь мог уйти с экрана во время запроса.
      if (isClosed) return;
      emit(
        state.copyWith(
          status: AuthFormStatus.success,
          user: user,
          clearError: true,
        ),
      );
    } on AuthFailure catch (failure) {
      _emitError(_messageFor(failure.type));
    } catch (_) {
      _emitError(_messageFor(AuthFailureType.unknown));
    }
  }

  String? _validate({
    required bool isPhone,
    required String phone,
    required String email,
    required String password,
  }) {
    if (isPhone) {
      if (!_phoneRegex.hasMatch(phone)) {
        return 'Введите 9 цифр номера после +992, без нуля в начале '
            '(например: 901234567)';
      }
    } else if (!_emailRegex.hasMatch(email)) {
      return 'Введите корректный Email, например example@rangshop.tj';
    }

    if (password.isEmpty) {
      return 'Введите пароль';
    }
    if (password.length < _minPasswordLength) {
      return 'Пароль должен содержать не менее $_minPasswordLength символов';
    }
    return null;
  }

  void _emitError(String message) {
    if (isClosed) return;
    emit(
      state.copyWith(
        status: AuthFormStatus.error,
        errorMessage: message,
        errorId: state.errorId + 1,
      ),
    );
  }

  String _messageFor(AuthFailureType type) {
    return switch (type) {
      AuthFailureType.invalidCredentials => 'Неверный логин или пароль',
      AuthFailureType.emailNotConfirmed =>
        'Email не подтверждён. Проверьте почту и перейдите по ссылке',
      AuthFailureType.tooManyRequests =>
        'Слишком много попыток. Повторите позже',
      AuthFailureType.network => 'Нет соединения с интернетом',
      AuthFailureType.unknown => 'Не удалось войти. Попробуйте снова',
    };
  }
}
