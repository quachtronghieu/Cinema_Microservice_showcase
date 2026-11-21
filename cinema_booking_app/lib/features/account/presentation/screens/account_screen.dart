import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';
import 'package:flutter/foundation.dart';

import '../../../../core/ui/cinema_theme.dart';
import '../../../../core/ui/cinema_widgets.dart';
import '../../../../core/ui/validators.dart';
import '../view_models/account_view_model.dart';

class AccountScreen extends StatelessWidget {
  const AccountScreen({super.key});
  @override
  Widget build(BuildContext c) {
    final vm = c.watch<AccountViewModel>();
    return Scaffold(
      appBar: AppBar(
        title: const Text('Tài khoản của tôi'),
        actions: [
          if (kDebugMode)
            IconButton(
              tooltip: 'Xem bắp/nước',
              icon: const Icon(Icons.fastfood_outlined),
              onPressed: () => c.push('/concessions'),
            ),
        ],
      ),
      body: !vm.loggedIn
          ? AsyncStateView(
              icon: Icons.lock_outline,
              message: 'Vui lòng đăng nhập để xem thông tin tài khoản',
              retry: () => c.go('/login'),
            )
          : ListView(
              padding: const EdgeInsets.all(20),
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(20),
                    child: Row(
                      children: [
                        CircleAvatar(
                          radius: 30,
                          child: Text(
                            _initials(vm.userSession?.username ?? ''),
                          ),
                        ),
                        const SizedBox(width: 16),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                '@${vm.userSession?.username ?? ''}',
                                style: Theme.of(c).textTheme.titleLarge,
                              ),
                              const SizedBox(height: 8),
                              Chip(
                                label: Text(
                                  vm.userSession?.role ?? 'Thành viên',
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                'Thông tin cá nhân',
                                style: Theme.of(c).textTheme.titleMedium,
                              ),
                            ),
                            TextButton.icon(
                              onPressed: () => showModalBottomSheet<void>(
                                context: c,
                                isScrollControlled: true,
                                builder: (_) => const _EditProfileSheet(),
                              ),
                              icon: const Icon(Icons.edit_outlined),
                              label: const Text('Chỉnh sửa'),
                            ),
                          ],
                        ),
                        const Divider(),
                        _info('Username', vm.profile?.username),
                        _info('Email', vm.profile?.email),
                        _info('Phone', vm.profile?.phone),
                        _info('Birthday', vm.profile?.dob),
                        _info('Gender', vm.profile?.sex),
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                Card(
                  child: ListTile(
                    leading: const Icon(Icons.logout, color: CinemaColors.red),
                    title: const Text(
                      'Đăng xuất',
                      style: TextStyle(color: CinemaColors.red),
                    ),
                    onTap: () async {
                      await c.read<AccountViewModel>().logout();
                      if (c.mounted) c.go('/login');
                    },
                  ),
                ),
              ],
            ),
      bottomNavigationBar: CinemaBottomNavigation(
        selectedIndex: 2,
        onItemSelected: (i) {
          if (i == 0) c.go('/movies');
          if (i == 1) c.go('/tickets');
        },
      ),
    );
  }

  static String _initials(String s) {
    final v = s.trim().toUpperCase();
    return v.isEmpty ? 'US' : v.substring(0, v.length.clamp(0, 2));
  }

  static Widget _info(String l, String? v) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 9),
    child: Row(
      children: [
        Expanded(
          child: Text(l, style: const TextStyle(color: CinemaColors.muted)),
        ),
        Expanded(
          child: Text(
            v?.isNotEmpty == true ? v! : '—',
            textAlign: TextAlign.end,
          ),
        ),
      ],
    ),
  );
}

class _EditProfileSheet extends StatefulWidget {
  const _EditProfileSheet();
  @override
  State<_EditProfileSheet> createState() => _EditProfileSheetState();
}

class _EditProfileSheetState extends State<_EditProfileSheet> {
  final _key = GlobalKey<FormState>();
  late final TextEditingController _phone, _dob;
  late String _sex;
  @override
  void initState() {
    super.initState();
    final p = context.read<AccountViewModel>().profile;
    _phone = TextEditingController(text: p?.phone ?? '');
    _dob = TextEditingController(text: p?.dob ?? '');
    _sex = p?.sex == 'Nữ' ? 'Nữ' : 'Nam';
  }

  @override
  void dispose() {
    _phone.dispose();
    _dob.dispose();
    super.dispose();
  }

  Future<void> _pick() async {
    final n = DateTime.now();
    final d = await showDatePicker(
      context: context,
      initialDate: DateTime(n.year - 18, n.month, n.day),
      firstDate: DateTime(1900),
      lastDate: n,
    );
    if (d != null) _dob.text = formatDateOfBirth(d);
  }

  Future<void> _save() async {
    if (!_key.currentState!.validate()) return;
    final ok = await context.read<AccountViewModel>().updateProfile(
      phone: _phone.text.trim(),
      dob: _dob.text,
      sex: _sex,
    );
    if (!mounted) return;
    if (ok) {
      Navigator.pop(context);
      showCinemaMessage(context, 'Cập nhật thông tin thành công!');
    }
  }

  @override
  Widget build(BuildContext c) => Padding(
    padding: EdgeInsets.fromLTRB(
      20,
      24,
      20,
      MediaQuery.viewInsetsOf(c).bottom + 24,
    ),
    child: Form(
      key: _key,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          TextFormField(
            controller: _phone,
            keyboardType: TextInputType.phone,
            decoration: const InputDecoration(labelText: 'Số điện thoại'),
            validator: validatePhone,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _dob,
            readOnly: true,
            onTap: _pick,
            decoration: const InputDecoration(labelText: 'Ngày sinh'),
            validator: (v) =>
                v == null || v.isEmpty ? 'Ngày sinh là bắt buộc.' : null,
          ),
          const SizedBox(height: 12),
          DropdownButtonFormField<String>(
            initialValue: _sex,
            decoration: const InputDecoration(labelText: 'Giới tính'),
            items: const [
              DropdownMenuItem(value: 'Nam', child: Text('Nam')),
              DropdownMenuItem(value: 'Nữ', child: Text('Nữ')),
            ],
            onChanged: (v) {
              if (v != null) setState(() => _sex = v);
            },
          ),
          const SizedBox(height: 20),
          FilledButton(onPressed: _save, child: const Text('Lưu thay đổi')),
        ],
      ),
    ),
  );
}
