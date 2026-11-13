// lib/features/auth/domain/repositories/auth_repository.dart


import 'package:equatable/equatable.dart';


enum UserRole {
  customer,
  owner,
  admin;


  static UserRole fromString(String? value) {
    return UserRole.values.firstWhere(
      (role) => role.name == value,
      orElse: () => UserRole.customer,
    );
  }
}


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


class AuthFailure implements Exception {
  const AuthFailure(this.type, {this.cause});


  final AuthFailureType type;
  final Object? cause;


  @override
  String toString() => 'AuthFailure($type, cause: $cause)';
}


abstract interface class AuthRepository {
  UserEntity? get currentUser;
  Stream<UserEntity?> get authStateChanges;


  Future<UserEntity> signInWithPhonePassword({
    required String phone,
    required String password,
  });


  Future<UserEntity> signInWithEmailPassword({
    required String email,
    required String password,
  });


  Future<void> sendPhoneOtp({required String phone});
  Future<UserEntity> verifyPhoneOtp({
    required String phone,
    required String code,
  });
  Future<void> signOut();
}
