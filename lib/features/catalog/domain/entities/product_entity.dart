class ProductEntity {
  const ProductEntity({
    required this.id,
    required this.name,
    required this.category,
    required this.priceDirams,
    this.oldPriceDirams,
    this.imageUrl,
    this.rating = 0,
    this.isFavorite = false,
  });

  final String id;
  final String name;

  /// 'clothes' | 'shoes' | 'accessories' | 'beauty'
  final String category;

  /// Цена СТРОГО в дирамах (целое число). 100 дирамов = 1 сомони.
  final int priceDirams;

  /// Старая цена в дирамах (для скидки). null — скидки нет.
  final int? oldPriceDirams;

  final String? imageUrl;
  final double rating;
  final bool isFavorite;

  bool get hasDiscount =>
      oldPriceDirams != null && oldPriceDirams! > priceDirams;

  int get discountPercent {
    if (!hasDiscount) return 0;
    return ((oldPriceDirams! - priceDirams) * 100 / oldPriceDirams!).round();
  }

  ProductEntity copyWith({
    String? id,
    String? name,
    String? category,
    int? priceDirams,
    int? oldPriceDirams,
    String? imageUrl,
    double? rating,
    bool? isFavorite,
  }) {
    return ProductEntity(
      id: id ?? this.id,
      name: name ?? this.name,
      category: category ?? this.category,
      priceDirams: priceDirams ?? this.priceDirams,
      oldPriceDirams: oldPriceDirams ?? this.oldPriceDirams,
      imageUrl: imageUrl ?? this.imageUrl,
      rating: rating ?? this.rating,
      isFavorite: isFavorite ?? this.isFavorite,
    );
  }
}
