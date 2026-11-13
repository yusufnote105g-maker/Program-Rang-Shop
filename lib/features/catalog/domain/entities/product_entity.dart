// lib/features/catalog/domain/entities/product_entity.dart


import 'package:equatable/equatable.dart';


class ProductEntity extends Equatable {
  const ProductEntity({
    required this.id,
    required this.title,
    required this.priceDirams,
    required this.imageUrl,
    required this.category,
  });


  final String id;
  final String title;
  final int priceDirams;
  final String imageUrl;
  final String category;


  @override
  List<Object?> get props => [id, title, priceDirams, imageUrl, category];
}
