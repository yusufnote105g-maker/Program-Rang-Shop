import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';
import 'package:rang_shop/features/catalog/domain/repositories/product_repository.dart';

class MockProductRepository implements ProductRepository {
  @override
  Future<List<ProductEntity>> getProducts() async {
    await Future<void>.delayed(const Duration(milliseconds: 400));

    // Все цены — в дирамах (12900 = 129 TJS).
    return const [
      ProductEntity(
        id: '1',
        name: 'Футболка Nike',
        category: 'clothes',
        priceDirams: 12900,
        oldPriceDirams: 19900, // скидка -35%
        rating: 4.8,
        isFavorite: true,
      ),
      ProductEntity(
        id: '2',
        name: 'Кроссовки',
        category: 'shoes',
        priceDirams: 59900,
        rating: 4.9,
      ),
      ProductEntity(
        id: '3',
        name: 'Худи Oversize',
        category: 'clothes',
        priceDirams: 39900,
        oldPriceDirams: 49900,
        rating: 4.7,
      ),
      ProductEntity(
        id: '4',
        name: 'Штаны Cargo',
        category: 'clothes',
        priceDirams: 26900,
        oldPriceDirams: 29900,
        rating: 4.8,
      ),
      ProductEntity(
        id: '5',
        name: 'Сумка кожаная',
        category: 'accessories',
        priceDirams: 45000,
        rating: 4.6,
      ),
      ProductEntity(
        id: '6',
        name: 'Помада матовая',
        category: 'beauty',
        priceDirams: 8900,
        oldPriceDirams: 11900,
        rating: 4.5,
      ),
      ProductEntity(
        id: '7',
        name: 'Платье вечернее',
        category: 'clothes',
        priceDirams: 52000,
        rating: 4.9,
      ),
      ProductEntity(
        id: '8',
        name: 'Часы классические',
        category: 'accessories',
        priceDirams: 89900,
        rating: 4.7,
      ),
    ];
  }
}
