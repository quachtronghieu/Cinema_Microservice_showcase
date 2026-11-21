import 'package:cinema_booking_app/features/showtime_and_seat/domain/entities/showtime.dart';

class ShowtimeDto {
  const ShowtimeDto({
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

  factory ShowtimeDto.fromJson(Map<String, dynamic> json) => ShowtimeDto(
    showtimeId: json['showtimeId'] as String,
    movieId: json['movieId'] as String,
    movieTitle: json['movieTitle'] as String,
    showDate: DateTime.parse(json['showDate'] as String),
    startTime: json['startTime'] as String,
    endTime: json['endTime'] as String,
    movieGenre: json['movieGenre'] as String?,
    movieDuration: json['movieDuration'] as int?,
    roomId: json['roomId'] as String?,
    roomType: json['roomType'] as String?,
    roomStatus: json['roomStatus'] as String?,
  );

  Showtime toEntity() => Showtime(
    showtimeId: showtimeId,
    movieId: movieId,
    movieTitle: movieTitle,
    showDate: showDate,
    startTime: startTime,
    endTime: endTime,
    movieGenre: movieGenre,
    movieDuration: movieDuration,
    roomId: roomId,
    roomType: roomType,
    roomStatus: roomStatus,
  );
}
