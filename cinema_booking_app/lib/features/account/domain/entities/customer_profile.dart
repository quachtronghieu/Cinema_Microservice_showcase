class CustomerProfile {
  const CustomerProfile({
    this.userId,
    required this.username,
    this.email,
    this.phone,
    this.dob,
    this.sex,
  });

  final String? userId;
  final String username;
  final String? email;
  final String? phone;
  final String? dob;
  final String? sex;

  Map<String, dynamic> toJson() => {
    'userId': userId,
    'username': username,
    'email': email,
    'phone': phone,
    'dob': dob,
    'sex': sex,
  };

  factory CustomerProfile.fromJson(Map<String, dynamic> json) =>
      CustomerProfile(
        userId: json['userId']?.toString(),
        username: json['username']?.toString() ?? '',
        email: json['email']?.toString(),
        phone: json['phone']?.toString(),
        dob: json['dob']?.toString(),
        sex: json['sex']?.toString(),
      );

  CustomerProfile copyWith({
    String? userId,
    String? username,
    String? email,
    String? phone,
    String? dob,
    String? sex,
  }) => CustomerProfile(
    userId: userId ?? this.userId,
    username: username ?? this.username,
    email: email ?? this.email,
    phone: phone ?? this.phone,
    dob: dob ?? this.dob,
    sex: sex ?? this.sex,
  );
}
