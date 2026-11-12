import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../domain/repositories/auth_repository.dart';

class MockAuthRepository implements AuthRepository {
  MockAuthRepository() {
    if (!kDebugMode) {
      throw StateError(
        'MockAuthRepository можно использовать только в debug-режиме',
      );
    }
  }

  static const String demoPassword = '123456';
  static const Duration _latency = Duration(milliseconds: 1200);

  final StreamController<UserEntity?> _controller =
      StreamController<UserEntity?>.broadcast();
  UserEntity? _currentUser;

  @override
  UserEntity? get currentUser => _currentUser;

  @override
  Stream<UserEntity?> get authStateChanges => _controller.stream;

  @override
  Future<UserEntity> signInWithPhonePassword({
    required String phone,
    required String password,
  }) {
    return _fakeSignIn(password: password, phone: phone);
  }

  @override
  Future<UserEntity> signInWithEmailPassword({
    required String email,
    required String password,
  }) {
    return _fakeSignIn(password: password, email: email);
  }

  @override
  Future<void> sendPhoneOtp({required String phone}) async {
    await Future<void>.delayed(_latency);
  }

  @override
  Future<UserEntity> verifyPhoneOtp({
    required String phone,
    required String code,
  }) {
    return _fakeSignIn(password: code, phone: phone);
  }

  @override
  Future<void> signOut() async {
    _currentUser = null;
    _controller.add(null);
  }

  Future<void> dispose() => _controller.close();

  Future<UserEntity> _fakeSignIn({
    required String password,
    String? phone,
    String? email,
  }) async {
    await Future<void>.delayed(_latency);

    if (password != demoPassword) {
      throw const AuthFailure(AuthFailureType.invalidCredentials);
    }

    final UserEntity user = UserEntity(
      id: 'mock-user-id',
      role: UserRole.customer,
      phone: phone,
      email: email,
    );
    _currentUser = user;
    if (!_controller.isClosed) _controller.add(user);
    return user;
  }
}

/* ============================================================================
   ЗАГОТОВКА ДЛЯ РЕАЛЬНОЙ РЕАЛИЗАЦИИ (SupabaseAuthRepository)
   lib/features/auth/data/repositories/auth_repository_impl.dart
   ============================================================================

   final SupabaseClient _client = Supabase.instance.client;

   Future<UserEntity> signInWithPhonePassword({phone, password}) async {
     try {
       final res = await _client.auth.signInWithPassword(
         phone: phone,
         password: password,
       );
       return _loadProfile(res.user);
     } on AuthException catch (e) {
       throw _map(e);
     } on SocketException catch (e) {
       throw AuthFailure(AuthFailureType.network, cause: e);
     }
   }

   Future<void> sendPhoneOtp({phone}) async {
     await _client.auth.signInWithOtp(phone: phone);
   }

   Future<UserEntity> verifyPhoneOtp({phone, code}) async {
     final res = await _client.auth.verifyOTP(
       type: OtpType.sms,
       phone: phone,
       token: code,
     );
     return _loadProfile(res.user);
   }

   Future<UserEntity> _loadProfile(User? user) async {
     if (user == null) throw const AuthFailure(AuthFailureType.invalidCredentials);
     final row = await _client
         .from('profiles')
         .select('role, full_name')
         .eq('id', user.id)
         .single();
     return UserEntity(
       id: user.id,
       role: UserRole.fromString(row['role'] as String?),
       phone: user.phone,
       email: user.email,
       fullName: row['full_name'] as String?,
     );
   }

   Stream<UserEntity?> get authStateChanges =>
       _client.auth.onAuthStateChange.asyncMap(
         (event) => event.session == null
             ? null
             : _loadProfile(event.session!.user),
       );
*/
