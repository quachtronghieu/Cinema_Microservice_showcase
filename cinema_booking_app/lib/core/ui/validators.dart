import 'package:intl/intl.dart';

final _emailPattern = RegExp(r'^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$');
final _phonePattern = RegExp(r'^[0-9]{9,11}$');
final _usernamePattern = RegExp(r'^(?=.*[a-z])(?=.*[A-Z])[A-Za-z0-9_]+$');
final _passwordPattern = RegExp(
  r'''^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?]).+$''',
);

String? validateUsername(String? value) {
  if (value == null || value.trim().isEmpty) return 'Username is required.';
  if (value.length < 2 || value.length > 20) {
    return 'Username must be 2-20 characters.';
  }
  if (!_usernamePattern.hasMatch(value)) {
    return 'Use upper/lowercase letters, numbers, or underscores.';
  }
  return null;
}

String? validatePassword(String? value) {
  if (value == null || value.isEmpty) return 'Password is required.';
  if (value.length < 6 || !_passwordPattern.hasMatch(value)) {
    return 'Use at least 6 characters with upper/lowercase, a digit, and a special character.';
  }
  return null;
}

String? validateEmail(String? value) {
  if (value == null || value.trim().isEmpty) return 'Email is required.';
  return _emailPattern.hasMatch(value) ? null : 'Invalid email.';
}

String? validatePhone(String? value) {
  if (value == null || value.trim().isEmpty) return 'Phone number is required.';
  return _phonePattern.hasMatch(value)
      ? null
      : 'Phone number must have 9-11 digits.';
}

String? validateDateOfBirth(DateTime? value) {
  if (value == null) return 'Date of birth is required.';
  final today = DateTime.now();
  if (value.isAfter(today)) return 'Date of birth cannot be later than today.';
  if (value.year < 1900) return 'Year of birth must be 1900 or later.';

  final age =
      today.year -
      value.year -
      ((today.month < value.month ||
              (today.month == value.month && today.day < value.day))
          ? 1
          : 0);
  return age >= 10 ? null : 'You must be at least 10 years old.';
}

String? validateDob(DateTime? value) => validateDateOfBirth(value);

String formatDateOfBirth(DateTime value) =>
    DateFormat('yyyy-MM-dd').format(value);
