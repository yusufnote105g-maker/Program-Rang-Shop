import 'package:supabase_flutter/supabase_flutter.dart';


class SupabaseService {
  const SupabaseService._();


  static bool _isInitialized = false;


  static const String _supabaseUrl = String.fromEnvironment(
    'SUPABASE_URL',
    defaultValue: 'https://kuyyrmxjplglqdfffyre.supabase.co',
  );


  static const String _supabaseAnonKey = String.fromEnvironment(
    'SUPABASE_ANON_KEY',
    defaultValue: 'sb_publishable_LmQxA96C5NsSJf2YV-CoFQ_GyHhhfw0',
  );


  static Future<void> initialize() async {
    if (_isInitialized) return;


    await Supabase.initialize(
      url: _supabaseUrl,
      anonKey: _supabaseAnonKey,
      authOptions: const FlutterAuthClientOptions(
        authFlowType: AuthFlowType.pkce,
      ),
    );


    _isInitialized = true;
  }


  static SupabaseClient get client {
    assert(
      _isInitialized,
      'SupabaseService.initialize() должен быть вызван перед обращением к SupabaseService.client',
    );
    return Supabase.instance.client;
  }


  static GoTrueClient get auth => client.auth;


  static SupabaseStorageClient get storage => client.storage;
}
