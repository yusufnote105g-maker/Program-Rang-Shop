// lib/features/auth/data/repositories/mock_auth_repository.dart


import 'dart:async';
import 'package:flutter/foundation.dart';
import 'package:rang_shop/features/auth/domain/repositories/auth_repository.dart';


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
    if (!_controller.isClosed) _controller.add(null);
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
