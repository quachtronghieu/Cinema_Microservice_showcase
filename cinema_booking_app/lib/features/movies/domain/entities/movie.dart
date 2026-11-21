class Movie {
  const Movie({
    required this.id,
    required this.title,
    required this.genres,
    required this.summary,
    required this.durationMinutes,
    required this.ageRating,
    this.releaseDate,
    this.posterUrl,
    this.trailerUrl,
  });

  final String id;
  final String title;
  final List<String> genres;
  final String summary;
  final int durationMinutes;
  final String ageRating;

  /// Ngày phát hành (chỉ phần ngày, không có giờ/múi giờ).
  final DateTime? releaseDate;
  final String? posterUrl;
  final String? trailerUrl;

  String get genreLabel => genres.join(' · ');
}
