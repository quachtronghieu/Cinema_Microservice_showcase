class Showtime {
  const Showtime({
    required this.showtimeId,
    required this.movieId,
    required this.movieTitle,
    required this.showDate,
    required this.startTime,
    required this.endTime,
    this.movieGenre,
    this.movieDuration,
    this.roomId,
    this.roomType,
    this.roomStatus,
  });

  final String showtimeId;
  final String movieId;
  final String movieTitle;
  final DateTime showDate;
  final String startTime;
  final String endTime;
  final String? movieGenre;
  final int? movieDuration;
  final String? roomId;
  final String? roomType;
  final String? roomStatus;
}
