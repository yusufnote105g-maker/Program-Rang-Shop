import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:rang_shop/features/catalog/domain/entities/product_entity.dart';


class AuthSessionCubit extends Cubit<bool> {
  AuthSessionCubit() : super(false);
  bool get isAuthorized => state;
  void signIn() => emit(true);
  void signOut() => emit(false);
}


@immutable
class CartItem {
  const CartItem({required this.product, this.quantity = 1});
  final ProductEntity product;
  final int quantity;
  int get totalDirams => product.priceDirams * quantity;
  CartItem copyWith({int? quantity}) => CartItem(product: product, quantity: quantity ?? this.quantity);
}


class CartCubit extends Cubit<List<CartItem>> {
  CartCubit() : super(const []);
  void add(ProductEntity product) {
    final index = state.indexWhere((item) => item.product.id == product.id);
    if (index == -1) {
      emit([...state, CartItem(product: product)]);
      return;
    }
    final updated = [...state];
    updated[index] = updated[index].copyWith(quantity: updated[index].quantity + 1);
    emit(updated);
  }
  void increment(String id) => emit([for (final item in state) if (item.product.id == id) item.copyWith(quantity: item.quantity + 1) else item]);
  void decrement(String id) => emit([for (final item in state) if (item.product.id == id && item.quantity > 1) item.copyWith(quantity: item.quantity - 1) else item]);
  void removeById(String id) => emit(state.where((item) => item.product.id != id).toList());
  void clear() => emit(const []);
  int get totalPriceDirams => state.fold(0, (sum, item) => sum + item.totalDirams);
  int get totalCount => state.fold(0, (sum, item) => sum + item.quantity);
}


class FavoritesCubit extends Cubit<List<ProductEntity>> {
  FavoritesCubit() : super(const []);
  void toggle(ProductEntity product) {
    if (isFavorite(product.id)) { removeById(product.id); } else { emit([...state, product]); }
  }
  bool isFavorite(String id) => state.any((product) => product.id == id);
  void removeById(String id) => emit(state.where((product) => product.id != id).toList());
}
