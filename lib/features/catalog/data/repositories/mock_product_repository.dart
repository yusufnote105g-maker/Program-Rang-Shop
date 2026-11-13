// lib/features/catalog/data/repositories/mock_product_repository.dart


import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';
import 'package:rang_shop/features/catalog/domain/repositories/product_repository.dart';


class MockProductRepository implements ProductRepository {
  const MockProductRepository();


  static const Duration _latency = Duration(milliseconds: 600);


  @override
  Future<List<ProductEntity>> getProducts() async {
    await Future<void>.delayed(_latency);
    return _products;
  }


  static const List<ProductEntity> _products = [
    ProductEntity(id: 'p1', title: 'Футболка оверсайз', priceDirams: 12000, imageUrl: 'https://picsum.photos/seed/rang-tshirt/400/400', category: 'Одежда'),
    ProductEntity(id: 'p2', title: 'Худи с капюшоном', priceDirams: 35000, imageUrl: 'https://picsum.photos/seed/rang-hoodie/400/400', category: 'Одежда'),
    ProductEntity(id: 'p3', title: 'Джинсы прямого кроя', priceDirams: 28000, imageUrl: 'https://picsum.photos/seed/rang-jeans/400/400', category: 'Одежда'),
    ProductEntity(id: 'p4', title: 'Платье летнее', priceDirams: 24950, imageUrl: 'https://picsum.photos/seed/rang-dress/400/400', category: 'Одежда'),
    ProductEntity(id: 'p5', title: 'Кроссовки беговые', priceDirams: 48000, imageUrl: 'https://picsum.photos/seed/rang-sneakers/400/400', category: 'Обувь'),
    ProductEntity(id: 'p6', title: 'Ботинки кожаные', priceDirams: 72000, imageUrl: 'https://picsum.photos/seed/rang-boots/400/400', category: 'Обувь'),
    ProductEntity(id: 'p7', title: 'Сандалии летние', priceDirams: 15000, imageUrl: 'https://picsum.photos/seed/rang-sandals/400/400', category: 'Обувь'),
    ProductEntity(id: 'p8', title: 'Лоферы классические', priceDirams: 39000, imageUrl: 'https://picsum.photos/seed/rang-loafers/400/400', category: 'Обувь'),
  ];
}
