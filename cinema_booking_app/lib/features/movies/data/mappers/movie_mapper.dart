import '../../../../core/config/api_config.dart';
import '../../domain/entities/movie.dart';
import '../dtos/movie_dto.dart';

// Backend lưu thể loại dạng chuỗi tự do (vd. "Hành động, Phiêu lưu").
final _genreSeparator = RegExp(r'\s*[,/|·;]\s*');

extension MovieDtoMapper on MovieDto {
  Movie toEntity() => Movie(
    id: movieId,
    title: title,
    genres: (genre ?? '')
        .split(_genreSeparator)
        .map((value) => value.trim())
        .where((value) => value.isNotEmpty)
        .toList(growable: false),
    summary: summary?.trim() ?? '',
    durationMinutes: duration ?? 0,
    ageRating: ageRating?.trim() ?? '',
    releaseDate: _parseDate(releaseDate),
    posterUrl: ApiConfig.resolveAssetUrl(img),
    trailerUrl: _normalizeTrailerUrl(trailer),
  );
}

/// Admin nhập link tự do nên chuẩn hóa về URL http(s) mở được, hoặc `null`.
String? _normalizeTrailerUrl(String? raw) {
  final value = raw?.trim() ?? '';
  if (value.isEmpty || value.contains(RegExp(r'\s'))) return null;
  if (RegExp(r'^https?://', caseSensitive: false).hasMatch(value)) {
    return value;
  }
  // File trailer do backend lưu, giống đường dẫn poster.
  if (value.startsWith('/')) return ApiConfig.resolveAssetUrl(value);
  // Thiếu scheme, ví dụ "youtu.be/abc" hoặc "www.youtube.com/watch?v=abc".
  if (value.contains('.') && !value.contains('://')) return 'https://$value';
  return null;
}

DateTime? _parseDate(String? value) {
  if (value == null || value.isEmpty) return null;
  final parsed = DateTime.tryParse(value);
  return parsed == null
      ? null
      : DateTime(parsed.year, parsed.month, parsed.day);
}
