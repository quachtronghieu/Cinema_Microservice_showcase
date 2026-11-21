import '../entities/cinema_models.dart';

abstract interface class ConcessionRepository {
  Future<List<CinemaConcession>> listConcessions();
}
