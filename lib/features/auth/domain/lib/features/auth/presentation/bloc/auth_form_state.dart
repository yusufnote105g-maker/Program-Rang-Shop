import 'package:equatable/equatable.dart';
import 'package:rang_shop/features/auth/domain/repositories/auth_repository.dart';

enum AuthFormStatus { initial, loading, success, error }

/// Состояние формы входа. Назван AuthFormState, чтобы не конфликтовать
/// с AuthState из supabase_flutter.
class AuthFormState extends Equatable {
  const AuthFormState({
    this.isPhoneLogin = true,
    this.obscurePassword = true,
    this.status = AuthFormStatus.initial,
    this.errorMessage,
    this.errorId = 0,
    this.user,
  });

  final bool isPhoneLogin;
  final bool obscurePassword;
  final AuthFormStatus status;
  final String? errorMessage;

  /// Счётчик ошибок. Растёт при каждой новой ошибке, поэтому две одинаковые
  /// ошибки подряд дают разные состояния и SnackBar показывается снова.
  final int errorId;

  /// Заполняется только при status == success.
  final UserEntity? user;

  bool get isLoading => status == AuthFormStatus.loading;

  AuthFormState copyWith({
    bool? isPhoneLogin,
    bool? obscurePassword,
    AuthFormStatus? status,
    String? errorMessage,
    int? errorId,
    UserEntity? user,
    bool clearError = false,
    bool clearUser = false,
  }) {
    return AuthFormState(
      isPhoneLogin: isPhoneLogin ?? this.isPhoneLogin,
      obscurePassword: obscurePassword ?? this.obscurePassword,
      status: status ?? this.status,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
      errorId: errorId ?? this.errorId,
      user: clearUser ? null : (user ?? this.user),
    );
  }

  @override
  List<Object?> get props => [
        isPhoneLogin,
        obscurePassword,
        status,
        errorMessage,
        errorId,
        user,
      ];
}
