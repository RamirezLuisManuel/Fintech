import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:9090/api/v1/auth';

  // Login
  static Future<Map<String, dynamic>> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );
      return _processResponse(response);
    } catch (e) {
      return {'success': false, 'message': 'Error de conexión: $e'};
    }
  }

  // Registro
  static Future<Map<String, dynamic>> register(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': email, 'password': password}),
      );
      return _processResponse(response);
    } catch (e) {
      return {'success': false, 'message': 'Error de conexión: $e'};
    }
  }

  // Procesar respuesta
  static Future<Map<String, dynamic>> _processResponse(http.Response response) async {
    if (response.statusCode == 200 || response.statusCode == 201) {
      final data = jsonDecode(response.body);
      
      // Guardar tokens en SharedPreferences
      final prefs = await SharedPreferences.getInstance();
      if (data['accessToken'] != null) {
        await prefs.setString('accessToken', data['accessToken']);
      }
      if (data['refreshToken'] != null) {
        await prefs.setString('refreshToken', data['refreshToken']);
      }
      if (data['numeroCuenta'] != null) {
        await prefs.setString('numeroCuenta', data['numeroCuenta']);
      }

      return {'success': true, 'data': data};
    } else {
      // Parsear mensaje de error
      String errorMsg = 'Error en el servidor (${response.statusCode})';
      try {
        final errorData = jsonDecode(response.body);
        if (errorData['mensaje'] != null) {
          errorMsg = errorData['mensaje'];
        }
      } catch (_) {}
      
      return {'success': false, 'message': errorMsg};
    }
  }

  // Cerrar Sesión
  static Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
  }

  // Verificar sesión activa
  static Future<bool> isLoggedIn() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.containsKey('accessToken');
  }
}
