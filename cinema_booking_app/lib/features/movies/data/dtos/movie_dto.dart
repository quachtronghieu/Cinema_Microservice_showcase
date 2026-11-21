/// Ánh xạ `MovieResponseDTO` của backend.
class MovieDto {
  const MovieDto({
    required this.movieId,
    required this.title,
    this.genre,
    this.summary,
    this.duration,
    this.releaseDate,
    this.ageRating,
    this.img,
    this.trailer,
  });

  final String movieId;
  final String title;
  final String? genre;
  final String? summary;
  final int? duration;

  /// Dạng `yyyy-MM-dd` (LocalDate của backend).
  final String? releaseDate;
  final String? ageRating;
  final String? img;
  final String? trailer;

  factory MovieDto.fromJson(Map<String, dynamic> json) {
    final rawId = json['movieID'] ?? json['movieId'] ?? json['id'];
    if (rawId == null) {
      throw const FormatException('Movie response is missing movieID.');
    }
    final rawDuration = json['duration'];
    return MovieDto(
      movieId: rawId.toString(),
      title: json['title']?.toString() ?? '',
      genre: json['genre']?.toString(),
      summary: json['summary']?.toString(),
      duration: rawDuration is num
          ? rawDuration.toInt()
          : int.tryParse(rawDuration?.toString() ?? ''),
      releaseDate: _readDate(json['releaseDate']),
      ageRating: json['ageRating']?.toString(),
      img: json['img']?.toString(),
      trailer: json['trailer']?.toString(),
    );
  }

  // Jackson có thể serialize LocalDate thành "2026-09-12" hoặc [2026, 9, 12]
  // tùy cấu hình WRITE_DATES_AS_TIMESTAMPS.
  static String? _readDate(dynamic value) {
    if (value is String) return value;
    if (value is List && value.length >= 3) {
      final parts = value.take(3).map((part) => part.toString()).toList();
      return '${parts[0]}-${parts[1].padLeft(2, '0')}-${parts[2].padLeft(2, '0')}';
    }
    return null;
  }
}
