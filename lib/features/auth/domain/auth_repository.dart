import 'package:equatable/equatable.dart';

/// Роли пользователя (соответствуют enum public.user_role в БД).
enum UserRole {
  customer,
  owner,
  admin;

  /// Неизвестное значение трактуется как самая слабая роль.
  static UserRole fromString(String? value) {
    return UserRole.values.firstWhere(
      (role) => role.name == value,
      orElse: () => UserRole.customer,
    );
  }
}

/// Доменная сущность пользователя. Не зависит от supabase_flutter.
class UserEntity extends Equatable {
  const UserEntity({
    required this.id,
    required this.role,
    this.phone,
    this.email,
    this.fullName,
  });

  final String id;
  final UserRole role;
  final String? phone;
  final String? email;
  final String? fullName;

  @override
  List<Object?> get props => [id, role, phone, email, fullName];
}

enum AuthFailureType {
  invalidCredentials,
  emailNotConfirmed,
  tooManyRequests,
  network,
  unknown,
}

/// Единственный тип ошибки, который репозиторий выбрасывает наружу.
/// Кубит не знает про AuthException, SocketException и т.д.
class AuthFailure implements Exception {
  const AuthFailure(this.type, {this.cause});

  final AuthFailureType type;

  /// Исходная ошибка, только для логов. В UI не показывается.
  final Object? cause;

  @override
  String toString() => 'AuthFailure($type, cause: $cause)';
}

/// Контракт авторизации. Номера телефонов передаются в формате E.164
/// (например, +992901234567).
///
/// Все методы при ошибке выбрасывают [AuthFailure].
abstract interface class AuthRepository {
  /// Текущий пользователь или null, если сессии нет.
  UserEntity? get currentUser;

  /// Поток изменений сессии (для GoRouter redirect и маршрутизации по ролям).
  Stream<UserEntity?> get authStateChanges;

  Future<UserEntity> signInWithPhonePassword({
    required String phone,
    required String password,
  });

  Future<UserEntity> signInWithEmailPassword({
    required String email,
    required String password,
  });

  /// Шаг 1 входа по SMS: отправка кода.
  Future<void> sendPhoneOtp({required String phone});

  /// Шаг 2 входа по SMS: проверка кода.
  Future<UserEntity> verifyPhoneOtp({
    required String phone,
    required String code,
  });

  Future<void> signOut();
}
