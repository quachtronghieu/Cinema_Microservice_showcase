import '../../domain/entities/customer_profile.dart';
import '../../domain/entities/user_session.dart';

class AccountState {
  const AccountState({
    this.isLoading = false,
    this.errorMessage,
    this.userSession,
    this.profile,
  });

  final bool isLoading;
  final String? errorMessage;
  final UserSession? userSession;
  final CustomerProfile? profile;
}
