import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';

Map<String, dynamic> catalogItem(
  int index, {
  bool active = true,
  bool images = false,
}) => {
  'concessionId':
      '00000000-0000-4000-8000-${index.toString().padLeft(12, '0')}',
  'type': index == 21 ? 'DR' : 'PC',
  'name': index == 21 ? 'Fixture Cola' : 'Fixture Popcorn $index',
  'description': 'Catalog fixture item.',
  'price': 25000,
  'img': images ? '/assets/img/concessions/coca.png' : '',
  'active': active,
  'version': 0,
};

Map<String, dynamic> catalogPage(
  List<Map<String, dynamic>> items, {
  int page = 0,
  int size = 20,
  int? total,
}) {
  final count = total ?? items.length;
  return {
    'items': items,
    'page': page,
    'size': size,
    'totalElements': count,
    'totalPages': (count + size - 1) ~/ size,
  };
}

class CatalogReply {
  CatalogReply(this.status, this.body, {this.image = false});
  final int status;
  final Object body;
  final bool image;
}

// Stand-in for the published API, never used by the production app.
class CatalogFixture {
  CatalogFixture({bool images = false})
    : items = List.generate(21, (i) => catalogItem(i + 1, images: images));

  static const token = 'fixture-catalog-user-token';
  final List<Map<String, dynamic>> items;
  final requests = <Uri>[];
  bool failCatalog = false;
  int imagesServed = 0;

  static const user = {
    'username': 'fixture_customer',
    'userId': 'fixture-user',
    'role': 'ROLE_USER',
    'roles': ['USER'],
  };

  CatalogReply route(String method, Uri uri, String? authorization) {
    requests.add(uri);
    if (uri.path.startsWith('/assets/img/concessions/')) {
      imagesServed++;
      return CatalogReply(
        200,
        base64Decode(
          'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a4E0AAAAASUVORK5CYII=',
        ),
        image: true,
      );
    }
    if (method == 'POST' && uri.path == '/api/auth/login') {
      return CatalogReply(200, {...user, 'accessToken': token});
    }
    if (authorization != 'Bearer $token') {
      return CatalogReply(401, {'code': 'UNAUTHORIZED'});
    }
    if (uri.path == '/api/auth/me') return CatalogReply(200, user);
    if (uri.path == '/api/v1/concessions') {
      if (failCatalog) {
        return CatalogReply(503, {'code': 'DEPENDENCY_UNAVAILABLE'});
      }
      final page = int.parse(uri.queryParameters['page'] ?? '0');
      final size = int.parse(uri.queryParameters['size'] ?? '20');
      final type = uri.queryParameters['type'];
      final available = items
          .where(
            (item) =>
                item['active'] == true &&
                (type == null || item['type'] == type),
          )
          .toList();
      final selected = available.skip(page * size).take(size).toList();
      return CatalogReply(
        200,
        catalogPage(selected, page: page, size: size, total: available.length),
      );
    }
    if (uri.path.startsWith('/api/v1/concessions/')) {
      final id = uri.pathSegments.last;
      final matches = items.where(
        (item) => item['concessionId'] == id && item['active'] == true,
      );
      if (matches.isNotEmpty) return CatalogReply(200, matches.single);
    }
    return CatalogReply(404, {'code': 'CONCESSION_NOT_FOUND'});
  }
}

class CatalogAdapter implements HttpClientAdapter {
  CatalogAdapter(this.fixture);
  final CatalogFixture fixture;
  Future<CatalogReply> Function(RequestOptions)? respond;
  final requests = <RequestOptions>[];

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final reply = respond != null
        ? await respond!(options)
        : fixture.route(
            options.method,
            options.uri,
            options.headers['Authorization'] as String?,
          );
    return ResponseBody.fromString(
      jsonEncode(reply.body),
      reply.status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
