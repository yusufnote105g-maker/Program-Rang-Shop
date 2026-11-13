// lib/features/catalog/domain/repositories/product_repository.dart


import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';


abstract interface class ProductRepository {
  Future<List<ProductEntity>> getProducts();
}
