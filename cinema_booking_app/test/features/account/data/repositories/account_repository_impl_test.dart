import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:cinema_booking_app/core/network/api_result.dart';
import 'package:cinema_booking_app/features/account/domain/entities/user_session.dart';
import 'package:cinema_booking_app/features/account/data/dtos/login_request_dto.dart';
import 'package:cinema_booking_app/features/account/data/dtos/reset_password_request_dto.dart';
import 'package:cinema_booking_app/features/account/data/repositories/account_repository_impl.dart';
import 'package:cinema_booking_app/features/account/data/services/account_api_service.dart';

class _MockAccountApiService extends AccountApiService {
  _MockAccountApiService() : super(dio: Dio());

  dynamic loginResponse;
  dynamic meResponse;
  dynamic forgotPasswordResponse;
  dynamic resetPasswordResponse;

  DioException? exceptionToThrow;

  @override
  Future<Response<dynamic>> login(LoginRequestDto request) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return Response(
      requestOptions: RequestOptions(path: '/api/auth/login'),
      statusCode: 200,
      data: loginResponse,
    );
  }

  @override
  Future<Response<dynamic>> getMe() async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return Response(
      requestOptions: RequestOptions(path: '/api/auth/me'),
      statusCode: 200,
      data: meResponse,
    );
  }

  @override
  Future<Response<dynamic>> forgotPassword(String email) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return Response(
      requestOptions: RequestOptions(path: '/api/auth/forgot-password'),
      statusCode: 200,
      data: forgotPasswordResponse,
    );
  }

  @override
  Future<Response<dynamic>> resetPassword(
    ResetPasswordRequestDto request,
  ) async {
    if (exceptionToThrow != null) throw exceptionToThrow!;
    return Response(
      requestOptions: RequestOptions(path: '/api/auth/reset-password'),
      statusCode: 200,
      data: resetPasswordResponse,
    );
  }
}

void main() {
  late _MockAccountApiService mockApiService;
  late AccountRepositoryImpl repository;

  setUp(() {
    mockApiService = _MockAccountApiService();
    repository = AccountRepositoryImpl(mockApiService);
  });

  group('AccountRepositoryImpl Backend Contract Tests', () {
    test('login extracts String userId CS000001 properly', () async {
      mockApiService.loginResponse = {
        'accessToken': 'fixture-login-token',
        'userId': 'CS000001',
        'username': 'thinhnp',
        'role': 'ROLE_USER',
      };

      final result = await repository.login('thinhnp', 'password123');

      expect(result, isA<Success>());
      final session = (result as Success).data;
      expect(session.userId, equals('CS000001'));
      expect(session.username, equals('thinhnp'));
      expect(session.role, equals('ROLE_USER'));
      expect(session.isAuthenticated, isTrue);
    });

    test('login rejects a response missing accessToken', () async {
      mockApiService.loginResponse = {
        'userId': 'CS000001',
        'username': 'fixture_customer',
        'role': 'ROLE_USER',
      };

      final result = await repository.login(
        'fixture_customer',
        'fixture-password',
      );

      expect(result, isA<FailureResult<UserSession>>());
    });

    test('getMe extracts String userId CS000001 properly', () async {
      mockApiService.meResponse = {
        'userId': 'CS000001',
        'username': 'thinhnp',
        'role': 'ROLE_USER',
      };

      final result = await repository.getMe();

      expect(result, isA<Success>());
      final session = (result as Success).data;
      expect(session.userId, equals('CS000001'));
      expect(session.username, equals('thinhnp'));
      expect(session.role, equals('ROLE_USER'));
      expect(session.isAuthenticated, isTrue);
    });

    test(
      'forgotPassword extracts nested error.message on 404 EMAIL_NOT_FOUND',
      () async {
        mockApiService.exceptionToThrow = DioException(
          requestOptions: RequestOptions(path: '/api/auth/forgot-password'),
          response: Response(
            requestOptions: RequestOptions(path: '/api/auth/forgot-password'),
            statusCode: 404,
            data: {
              'error': {
                'code': 'EMAIL_NOT_FOUND',
                'message': 'No account found for this email.',
              },
            },
          ),
        );

        final result = await repository.forgotPassword('nonexistent@gmail.com');

        expect(result, isA<FailureResult>());
        final failure = (result as FailureResult).failure;
        expect(failure.message, equals('No account found for this email.'));
        expect(failure.statusCode, equals(404));
      },
    );

    test(
      'resetPassword extracts nested error.message on 400 INVALID_RESET_LINK',
      () async {
        mockApiService.exceptionToThrow = DioException(
          requestOptions: RequestOptions(path: '/api/auth/reset-password'),
          response: Response(
            requestOptions: RequestOptions(path: '/api/auth/reset-password'),
            statusCode: 400,
            data: {
              'error': {
                'code': 'INVALID_RESET_LINK',
                'message': 'This reset link is no longer valid.',
              },
            },
          ),
        );

        final result = await repository.resetPassword(
          id: 'CS000001',
          resetTimestamp: 1712000000000,
          resetCode: 'invalid_code',
          newPassword: 'NewChangeMe_Local_9!',
          confirmPassword: 'NewChangeMe_Local_9!',
        );

        expect(result, isA<FailureResult>());
        final failure = (result as FailureResult).failure;
        expect(failure.message, equals('This reset link is no longer valid.'));
        expect(failure.statusCode, equals(400));
      },
    );

    test('resetPassword extracts field details in failure.errors on 422 VALIDATION_ERROR', () async {
      mockApiService.exceptionToThrow = DioException(
        requestOptions: RequestOptions(path: '/api/auth/reset-password'),
        response: Response(
          requestOptions: RequestOptions(path: '/api/auth/reset-password'),
          statusCode: 422,
          data: {
            'error': {
              'code': 'VALIDATION_ERROR',
              'message': 'Invalid request data',
              'details': {'confirmPassword': 'Passwords must match.'},
            },
          },
        ),
      );

      final result = await repository.resetPassword(
        id: 'CS000001',
        resetTimestamp: 1712000000000,
        resetCode: 'valid_code',
        newPassword: 'NewChangeMe_Local_9!',
        confirmPassword: 'MismatchChangeMe_Local_9!',
      );

      expect(result, isA<FailureResult>());
      final failure = (result as FailureResult).failure;
      expect(failure.statusCode, equals(422));
      expect(failure.errors, isNotNull);
      expect(
        failure.errors!['confirmPassword'],
        equals('Passwords must match.'),
      );
    });
  });
}
