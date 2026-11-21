import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../view_models/concession_view_model.dart';

class CartNotice extends StatelessWidget {
  const CartNotice({super.key});

  @override
  Widget build(BuildContext context) {
    final vm = context.watch<ConcessionsViewModel>();
    final message = vm.cartNotice;
    if (message == null) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          Expanded(child: Text(message)),
          IconButton(
            tooltip: 'Đóng thông báo giỏ hàng',
            onPressed: vm.dismissCartNotice,
            icon: const Icon(Icons.close),
          ),
        ],
      ),
    );
  }
}
