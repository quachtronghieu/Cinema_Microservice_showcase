import '../errors/failures.dart';

sealed class ApiResult<T> {
  const ApiResult();

  static ApiResult<T> success<T>(T data) => Success<T>(data);

  static ApiResult<T> failure<T>(Failure failure) => FailureResult<T>(failure);
}

final class Success<T> extends ApiResult<T> {
  const Success(this.data);

  final T data;
}

final class FailureResult<T> extends ApiResult<T> {
  const FailureResult(this.failure);

  final Failure failure;
}
